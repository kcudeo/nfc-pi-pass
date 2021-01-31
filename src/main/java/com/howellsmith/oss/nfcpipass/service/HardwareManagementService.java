package com.howellsmith.oss.nfcpipass.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.howellsmith.oss.nfcpipass.model.event.DeviceConnectionStatusEvent;
import com.howellsmith.oss.nfcpipass.model.event.TagInFieldEvent;
import com.howellsmith.oss.nfcpipass.model.log.error.ExceptionalErrorLog;
import com.howellsmith.oss.nfcpipass.ufr.UfrDevice;
import com.sun.jna.ptr.ByteByReference;
import lombok.Builder;
import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.DEVICE_ID;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.DEVICE_STATUS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.LOG_MARKER_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.LOG_MSG_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_CRITICAL_INFORMATION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_UNEXPECTED_EXCEPTION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_UNEXPECTED_READER_STATUS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_GET_READER_TYPE_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_SUCCESS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_READ_NDEF_FAIL;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_SLEEP_INTERRUPTED_DURING_RESET;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_UNEXPECTED_READER_STATUS;
import static com.howellsmith.oss.nfcpipass.service.UnifiedLoggingService.getStackMap;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.DL_OK;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.NO_CARD;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

/**
 * The hardware management service is responsible for maintaining the connection to the usb nfc hardware, reading
 * the available data from the tags in field and broadcasting the results to the rest of the application.
 */
@Service
public class HardwareManagementService {

    private static final String TO_8CHAR_PADDED_HEX = "0x%08X";
    private static final int UFR_NANO_ONLINE = 0xD1390222;
    private static final long UFR_NANO_ONLINE_RESET_TIME = 20000L;
    private static final int UFR_NANO = 0xD1380022;
    private static final long UFR_NANO_RESET_TIME = 5000L;
    private static final long UFR_GENERIC_DEVICE = 10000L;

    private final UnifiedLoggingService log;
    private final UfrDevice device;
    private final ApplicationEventPublisher publisher;

    private final int[] workingDeviceType = new int[2];
    private final ByteByReference cardId = new ByteByReference();
    private final ByteByReference uidSize = new ByteByReference();
    private final byte[] uid = new byte[8];

    private final AtomicBoolean connected = new AtomicBoolean(false);

    public HardwareManagementService(UnifiedLoggingService log, UfrDevice device, ApplicationEventPublisher publisher) {
        this.log = log;
        this.device = device;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelay = 1000L)
    private void poll() throws InterruptedException {

        if (!confirmDeviceConnection()) return;

        int workingStatus;
        switch (workingStatus = device.GetCardIdEx(cardId, uid, uidSize)) {
            case NO_CARD:
                publisher.publishEvent(new TagInFieldEvent(this, false, null, null));
                break;
            case DL_OK:
                var data = new byte[1000];
                if ((workingStatus = device.ReadNdefRecord_Text(data)) == DL_OK) {
                    publisher.publishEvent(new TagInFieldEvent(this, true, uidToLong(uid), new String(data).trim()));
                } else {
                    publisher.publishEvent(new TagInFieldEvent(this, true, uidToLong(uid), null));
                    log.error(DeviceStatusLogWrapper.builder()
                            .marker(MARKER_UNEXPECTED_READER_STATUS)
                            .message(MESSAGE_NFC_READER_READ_NDEF_FAIL)
                            .deviceId(String.format(TO_8CHAR_PADDED_HEX, workingDeviceType[0]))
                            .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                            .build());
                }
                break;
            default:
                publisher.publishEvent(new TagInFieldEvent(this, false, null, null));
                log.error(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_UNEXPECTED_READER_STATUS)
                        .message(MESSAGE_UNEXPECTED_READER_STATUS)
                        .deviceId(String.format(TO_8CHAR_PADDED_HEX, workingDeviceType[0]))
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());
        }
    }

    /**
     * The uFR nfc hardware may be connected or disconnected at any time regardless of stored state. Method confirms
     * that the hardware is in fact connected at the time of execution.
     *
     * @return True for connected, False otherwise.
     */
    private boolean confirmDeviceConnection() throws InterruptedException {

        var workingStatus = 0;
        if (connected.get()) {
            if ((workingStatus = device.GetReaderType(workingDeviceType)) == DL_OK) {
                publisher.publishEvent(new DeviceConnectionStatusEvent(this, true));
                return true;
            } else {

                log.error(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_CRITICAL_INFORMATION)
                        .message(MESSAGE_NFC_GET_READER_TYPE_FAILURE)
                        .deviceId(String.format(TO_8CHAR_PADDED_HEX, workingDeviceType[0]))
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());

                connected.set(false);
                device.ReaderReset();
                device.ReaderClose();

                publisher.publishEvent(new DeviceConnectionStatusEvent(this, false));

                waitForReset();
                return false;
            }
        } else {
            if ((workingStatus = device.ReaderOpen()) == DL_OK) {

                log.info(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_CRITICAL_INFORMATION)
                        .message(MESSAGE_NFC_READER_CONNECT_SUCCESS)
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());

                connected.set(true);
                publisher.publishEvent(new DeviceConnectionStatusEvent(this, true));

                return true;
            } else {

                log.error(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_CRITICAL_INFORMATION)
                        .message(MESSAGE_NFC_READER_CONNECT_FAILURE)
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());

                publisher.publishEvent(new DeviceConnectionStatusEvent(this, false));
                waitForReset();
                return false;
            }
        }
    }

    /**
     * Extracted method to wait on the device to initialize/reset.
     */
    private void waitForReset() throws InterruptedException {
        try {
            switch (workingDeviceType[0]) {
                case UFR_NANO:
                    Thread.sleep(UFR_NANO_RESET_TIME);
                    break;
                case UFR_NANO_ONLINE:
                    Thread.sleep(UFR_NANO_ONLINE_RESET_TIME);
                    break;
                default:
                    Thread.sleep(UFR_GENERIC_DEVICE);
            }
        } catch (InterruptedException e) {
            log.error(ExceptionalErrorLog.builder()
                    .marker(MARKER_UNEXPECTED_EXCEPTION)
                    .message(MESSAGE_SLEEP_INTERRUPTED_DURING_RESET)
                    .exceptionMessage(getMessage(e))
                    .stackTrace(getStackMap(e))
                    .build());

            Thread.currentThread().interrupt();
        }
    }

    /**
     * UIDs read from the hardware are read in little endian. This corrects that and converts to long.
     *
     * @param uid The nfc media uid as read from the nfc device.
     * @return Returns a long.
     */
    private long uidToLong(byte[] uid) {
        return new BigInteger(new byte[]{uid[7], uid[6], uid[5], uid[4], uid[3], uid[2], uid[1], uid[0]})
                .longValueExact();
    }

    /**
     * Simple device status log wrapper object.
     */
    @Builder
    @Data
    static class DeviceStatusLogWrapper {

        @JsonProperty(LOG_MARKER_KEY)
        private String marker;

        @JsonProperty(LOG_MSG_KEY)
        private String message;

        @JsonProperty(DEVICE_ID)
        private String deviceId;

        @JsonProperty(DEVICE_STATUS)
        private String status;
    }
}
