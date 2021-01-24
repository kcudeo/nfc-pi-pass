package com.howellsmith.oss.nfcpipass.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.howellsmith.oss.nfcpipass.model.log.error.ExceptionalErrorLog;
import com.howellsmith.oss.nfcpipass.ufr.UfrDevice;
import lombok.Builder;
import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.DEVICE_ID;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.DEVICE_STATUS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.LOG_MARKER_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.LOG_MSG_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_CRITICAL_INFORMATION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_UNEXPECTED_EXCEPTION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_GET_READER_TYPE_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_SUCCESS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_SLEEP_INTERRUPTED_DURING_RESET;
import static com.howellsmith.oss.nfcpipass.service.UnifiedLoggingService.getStackMap;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.DL_OK;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

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

    private final int connectedDevice = 0;
    private final int[] workingDeviceType = new int[2];

    private final AtomicBoolean connected = new AtomicBoolean(false);

    public HardwareManagementService(UnifiedLoggingService log, UfrDevice device) {
        this.log = log;
        this.device = device;
    }

    @Scheduled(fixedDelay = 1000L)
    private void poll() {

        if (!confirmDeviceConnection()) {
            return;
        }

        // check for nfc...


        log.info(Map.of(LOG_MARKER_KEY, "POLL NOTICE"));
    }

    private boolean confirmDeviceConnection() {

        var workingStatus = 0;
        if (connected.get()) {
            if ((workingStatus = device.GetReaderType(workingDeviceType)) == DL_OK) {
                return true;
            } else {

                log.error(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_CRITICAL_INFORMATION)
                        .status(MESSAGE_NFC_GET_READER_TYPE_FAILURE)
                        .deviceId(String.format(TO_8CHAR_PADDED_HEX, workingDeviceType[0]))
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());

                connected.set(false);
                device.ReaderReset();
                device.ReaderClose();

                // TODO: Broadcast Event

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
                }

                return false;
            }
        } else {
            if ((workingStatus = device.ReaderOpen()) == DL_OK) {

                log.info(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_CRITICAL_INFORMATION)
                        .status(MESSAGE_NFC_READER_CONNECT_SUCCESS)
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());

                connected.set(true);

                // TODO: Broadcast Event

                return true;
            } else {

                log.info(DeviceStatusLogWrapper.builder()
                        .marker(MARKER_CRITICAL_INFORMATION)
                        .status(MESSAGE_NFC_READER_CONNECT_FAILURE)
                        .status(String.format(TO_8CHAR_PADDED_HEX, workingStatus))
                        .build());

                connected.set(false);
                return false;
            }
        }
    }

    /**
     * Simple device status log wrapper object.
     */
    @Builder
    @Data
    private static class DeviceStatusLogWrapper {

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
