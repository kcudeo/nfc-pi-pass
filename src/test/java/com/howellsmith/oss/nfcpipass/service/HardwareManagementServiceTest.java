package com.howellsmith.oss.nfcpipass.service;

import com.howellsmith.oss.nfcpipass.model.event.DeviceConnectionStatusEvent;
import com.howellsmith.oss.nfcpipass.model.event.TagInFieldEvent;
import com.howellsmith.oss.nfcpipass.ufr.UfrDevice;
import com.sun.jna.ptr.ByteByReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_CRITICAL_INFORMATION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_UNEXPECTED_READER_STATUS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_GET_READER_TYPE_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_SUCCESS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_UNEXPECTED_READER_STATUS;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.DL_OK;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.NO_CARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class HardwareManagementServiceTest {

    private static UnifiedLoggingService log;
    private static UfrDevice device;
    private static ApplicationEventPublisher publisher;
    private static HardwareManagementService service;
    private static Method poll;

    @BeforeAll
    static void setup() throws NoSuchMethodException {

        // define mocks
        log = Mockito.mock(UnifiedLoggingService.class);
        device = Mockito.mock(UfrDevice.class);
        publisher = Mockito.mock(ApplicationEventPublisher.class);

        // setup service
        service = new HardwareManagementService(log, device, publisher);

        // enable access
        poll = service.getClass().getDeclaredMethod("poll");
        poll.setAccessible(true);

        // define default handling
        Mockito.doNothing().when(publisher).publishEvent(any());
        Mockito.doNothing().when(log).info(any());
        Mockito.doNothing().when(log).error(any());
    }

    @BeforeEach
    public void reset() {
        Mockito.reset(device);
    }

    @Test
    public void test_connection_workflow() throws InvocationTargetException, IllegalAccessException {

        //<editor-fold desc="Path -> Yes NFC Connect, Yes proper NFC card in field.">

        // Define connection to open DL_OK
        Mockito.when(device.ReaderOpen()).thenReturn(DL_OK);

        // Define return DL_OK and populate referenced variables on GetCardIdEx
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            var cardId = invocation.getArgument(0, ByteByReference.class);
            byte[] uid = invocation.getArgument(1);
            var uidSize = invocation.getArgument(2, ByteByReference.class);

            cardId.setValue((byte) 0xFF);
            for (int i = 0; i < 8; i++) {
                uid[i] = (byte) i;
            }
            uidSize.setValue((byte) 0x08);

            return DL_OK;
        }).when(device).GetCardIdEx(any(), any(), any());

        // Define return DL_OK and populate referenced variable with fake NDEF Text data
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            byte[] uid = invocation.getArgument(0);
            var text = "TESTING".getBytes();

            System.arraycopy(text, 0, uid, 0, text.length);

            return DL_OK;
        }).when(device).ReadNdefRecord_Text(any());

        // Setup argument captors for log and event validation
        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        // Emulate the poll for the first time
        poll.invoke(service);

        // Install the captors
        Mockito.verify(publisher, times(2)).publishEvent(publishCaptor.capture());
        Mockito.verify(log, times(1)).info(logCaptor.capture());

        // Get the data
        var capturedPublishEvents = publishCaptor.getAllValues();
        var capturedLogEvents = logCaptor.getAllValues();

        DeviceConnectionStatusEvent expectedEvent0 = null;
        TagInFieldEvent expectedEvent1 = null;

        for (var event : capturedPublishEvents) {
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
        }

        // Make assertions
        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertEquals("TESTING", expectedEvent1.getNdef());
        assertTrue(expectedEvent1.isDeviceInField());
        assertEquals(0x0706050403020100L, expectedEvent1.getUid());

        var statusWrapper = capturedLogEvents.get(0);
        assertEquals(MARKER_CRITICAL_INFORMATION, statusWrapper.getMarker());
        assertEquals(MESSAGE_NFC_READER_CONNECT_SUCCESS, statusWrapper.getMessage());
        assertEquals("0x00000000", statusWrapper.getStatus());
        assertNull(statusWrapper.getDeviceId());

        Mockito.verify(device, times(1)).ReaderOpen();
        Mockito.verify(device, times(1)).GetCardIdEx(any(), any(), any());
        Mockito.verify(device, times(1)).ReadNdefRecord_Text(any());
        //</editor-fold>

        //<editor-fold desc="Path -> Yes NFC Connect, Yes NFC card in field, No NDEF text record.">
        Mockito.reset(device);

        // Define get reader type return.
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            int[] uid = invocation.getArgument(0);
            uid[0] = 1;
            uid[1] = 1;

            return DL_OK;
        }).when(device).GetReaderType(any(int[].class));
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            var cardId = invocation.getArgument(0, ByteByReference.class);
            byte[] uid = invocation.getArgument(1);
            var uidSize = invocation.getArgument(2, ByteByReference.class);

            cardId.setValue((byte) 0xFF);
            for (int i = 0; i < 8; i++) {
                uid[i] = (byte) i;
            }
            uidSize.setValue((byte) 0x08);

            return DL_OK;
        }).when(device).GetCardIdEx(any(), any(), any());
        when(device.ReadNdefRecord_Text(any())).thenReturn(0xFF);

        // Setup argument captors for log and event validation
        var publishCaptor_BADCARD = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor_BADCARD = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        poll.invoke(service);

        Mockito.verify(publisher, times(4)).publishEvent(publishCaptor_BADCARD.capture());
        Mockito.verify(log, times(1)).error(logCaptor_BADCARD.capture());

        var capturedPublishEvents_BADCARD = publishCaptor_BADCARD.getAllValues();
        capturedPublishEvents_BADCARD.removeAll(capturedPublishEvents);

        var capturedLogEvent_BADCARD = logCaptor_BADCARD.getAllValues();
        capturedLogEvent_BADCARD.removeAll(capturedLogEvents);

        assertEquals(1, capturedLogEvent_BADCARD.size());
        assertEquals(2, capturedPublishEvents_BADCARD.size());

        expectedEvent0 = null;
        expectedEvent1 = null;

        for (var event : capturedPublishEvents_BADCARD) {
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
        }
        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertTrue(expectedEvent1.isDeviceInField());
        assertEquals(0x0706050403020100L, expectedEvent1.getUid());
        assertNull(expectedEvent1.getNdef());
        //</editor-fold>

        //<editor-fold desc="Path -> Yes NFC Connect, No NFC card in field.">
        Mockito.reset(device);

        // Define get reader type return.
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            int[] uid = invocation.getArgument(0);
            uid[0] = 1;
            uid[1] = 1;

            return DL_OK;
        }).when(device).GetReaderType(any(int[].class));
        when(device.GetCardIdEx(any(), any(), any())).thenReturn(NO_CARD);

        // Setup argument captors for log and event validation
        var publishCaptor_NOCARD = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor_NOCARD = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        poll.invoke(service);

        Mockito.verify(publisher, times(6)).publishEvent(publishCaptor_NOCARD.capture());
        Mockito.verify(log, times(1)).info(logCaptor_NOCARD.capture());

        var capturedPublishEvents_NOCARD = publishCaptor_NOCARD.getAllValues();
        capturedPublishEvents_NOCARD.removeAll(capturedPublishEvents);
        capturedPublishEvents_NOCARD.removeAll(capturedPublishEvents_BADCARD);

        var capturedLogEvent_NOCARD = logCaptor_NOCARD.getAllValues();
        capturedLogEvent_NOCARD.removeAll(capturedLogEvents);
        capturedLogEvent_NOCARD.removeAll(capturedLogEvent_BADCARD);

        assertEquals(0, capturedLogEvent_NOCARD.size());
        assertEquals(2, capturedPublishEvents_NOCARD.size());

        expectedEvent0 = null;
        expectedEvent1 = null;

        for (var event : capturedPublishEvents_NOCARD) {
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
        }
        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertFalse(expectedEvent1.isDeviceInField());
        assertNull(expectedEvent1.getUid());
        assertNull(expectedEvent1.getNdef());
        //</editor-fold>

        //<editor-fold desc="Path -> Yes NFC Connect, Unknown Status from NFC hardware">
        Mockito.reset(device);

        // Define get reader type return.
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            int[] uid = invocation.getArgument(0);
            uid[0] = 1;
            uid[1] = 1;

            return DL_OK;
        }).when(device).GetReaderType(any(int[].class));
        when(device.GetCardIdEx(any(), any(), any())).thenReturn(0xFF);

        // Setup argument captors for log and event validation
        var publishCaptor_UNKNOWN = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor_UNKNOWN = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        poll.invoke(service);

        Mockito.verify(publisher, times(8)).publishEvent(publishCaptor_UNKNOWN.capture());
        Mockito.verify(log, times(2)).error(logCaptor_UNKNOWN.capture());

        var capturedPublishEvents_UNKNOWN = publishCaptor_UNKNOWN.getAllValues();
        capturedPublishEvents_UNKNOWN.removeAll(capturedPublishEvents);
        capturedPublishEvents_UNKNOWN.removeAll(capturedPublishEvents_BADCARD);
        capturedPublishEvents_UNKNOWN.removeAll(capturedPublishEvents_NOCARD);

        var capturedLogEvent_UNKNOWN = logCaptor_UNKNOWN.getAllValues();
        capturedLogEvent_UNKNOWN.removeAll(capturedLogEvents);
        capturedLogEvent_UNKNOWN.removeAll(capturedLogEvent_BADCARD);
        capturedLogEvent_UNKNOWN.removeAll(capturedLogEvent_NOCARD);

        assertEquals(1, capturedLogEvent_UNKNOWN.size());
        assertEquals(2, capturedPublishEvents_UNKNOWN.size());

        expectedEvent0 = null;
        expectedEvent1 = null;

        for (var event : capturedPublishEvents_UNKNOWN) {
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
        }
        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertFalse(expectedEvent1.isDeviceInField());
        assertNull(expectedEvent1.getUid());
        assertNull(expectedEvent1.getNdef());

        var statusWrapper_UNKNOWN = capturedLogEvent_UNKNOWN.get(0);
        assertEquals(MARKER_UNEXPECTED_READER_STATUS, statusWrapper_UNKNOWN.getMarker());
        assertEquals(MESSAGE_UNEXPECTED_READER_STATUS, statusWrapper_UNKNOWN.getMessage());
        assertEquals("0x00000001", statusWrapper_UNKNOWN.getDeviceId());
        assertEquals("0x000000FF", statusWrapper_UNKNOWN.getStatus());
        //</editor-fold>
        
        //<editor-fold desc="Path -> Previous Connect Success to No NFC Device">
        Mockito.reset(device);

        // Define get reader type return.
        when(device.GetReaderType(any(int[].class))).thenReturn(0xFF);

        // Setup argument captors for log and event validation
        var publishCaptor_FAILED_READER_TYPE = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor_FAILED_READER_TYPE = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        poll.invoke(service);

        Mockito.verify(publisher, times(9)).publishEvent(publishCaptor_FAILED_READER_TYPE.capture());
        Mockito.verify(log, times(3)).error(logCaptor_FAILED_READER_TYPE.capture());

        var capturedPublishEvents_FAILED_READER_TYPE = publishCaptor_FAILED_READER_TYPE.getAllValues();
        capturedPublishEvents_FAILED_READER_TYPE.removeAll(capturedPublishEvents);
        capturedPublishEvents_FAILED_READER_TYPE.removeAll(capturedPublishEvents_BADCARD);
        capturedPublishEvents_FAILED_READER_TYPE.removeAll(capturedPublishEvents_NOCARD);
        capturedPublishEvents_FAILED_READER_TYPE.removeAll(capturedPublishEvents_UNKNOWN);

        var capturedLogEvent_FAILED_READER_TYPE = logCaptor_FAILED_READER_TYPE.getAllValues();
        capturedLogEvent_FAILED_READER_TYPE.removeAll(capturedLogEvents);
        capturedLogEvent_FAILED_READER_TYPE.removeAll(capturedLogEvent_BADCARD);
        capturedLogEvent_FAILED_READER_TYPE.removeAll(capturedLogEvent_NOCARD);
        capturedLogEvent_FAILED_READER_TYPE.removeAll(capturedLogEvent_UNKNOWN);

        assertEquals(1, capturedLogEvent_FAILED_READER_TYPE.size());
        assertEquals(1, capturedPublishEvents_FAILED_READER_TYPE.size());

        expectedEvent0 = null;

        for (var event : capturedPublishEvents_FAILED_READER_TYPE) {
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
        }
        assertNotNull(expectedEvent0);
        assertFalse(expectedEvent0.isConnected());

        var statusWrapper_FAILED_READER_TYPE = capturedLogEvent_FAILED_READER_TYPE.get(0);
        assertEquals(MARKER_CRITICAL_INFORMATION, statusWrapper_FAILED_READER_TYPE.getMarker());
        assertEquals(MESSAGE_NFC_GET_READER_TYPE_FAILURE, statusWrapper_FAILED_READER_TYPE.getMessage());
        assertEquals("0x00000001", statusWrapper_FAILED_READER_TYPE.getDeviceId());
        assertEquals("0x000000FF", statusWrapper_FAILED_READER_TYPE.getStatus());
        //</editor-fold>

        //<editor-fold desc="Path -> Failed to Connect to NFC Hardware">
        Mockito.reset(device);

        when(device.ReaderOpen()).thenReturn(0xFF);

        // Setup argument captors for log and event validation
        var publishCaptor_FAILED_CONNECT = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor_FAILED_CONNECT = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        poll.invoke(service);

        Mockito.verify(publisher, times(10)).publishEvent(publishCaptor_FAILED_CONNECT.capture());
        Mockito.verify(log, times(4)).error(logCaptor_FAILED_CONNECT.capture());

        var capturedPublishEvents_FAILED_CONNECT = publishCaptor_FAILED_CONNECT.getAllValues();
        capturedPublishEvents_FAILED_CONNECT.removeAll(capturedPublishEvents);
        capturedPublishEvents_FAILED_CONNECT.removeAll(capturedPublishEvents_BADCARD);
        capturedPublishEvents_FAILED_CONNECT.removeAll(capturedPublishEvents_NOCARD);
        capturedPublishEvents_FAILED_CONNECT.removeAll(capturedPublishEvents_UNKNOWN);
        capturedPublishEvents_FAILED_CONNECT.removeAll(capturedPublishEvents_FAILED_READER_TYPE);

        var capturedLogEvent_FAILED_CONNECT = logCaptor_FAILED_CONNECT.getAllValues();
        capturedLogEvent_FAILED_CONNECT.removeAll(capturedLogEvents);
        capturedLogEvent_FAILED_CONNECT.removeAll(capturedLogEvent_BADCARD);
        capturedLogEvent_FAILED_CONNECT.removeAll(capturedLogEvent_NOCARD);
        capturedLogEvent_FAILED_CONNECT.removeAll(capturedLogEvent_UNKNOWN);
        capturedLogEvent_FAILED_CONNECT.removeAll(capturedLogEvent_FAILED_READER_TYPE);

        assertEquals(1, capturedLogEvent_FAILED_CONNECT.size());
        assertEquals(1, capturedPublishEvents_FAILED_CONNECT.size());

        expectedEvent0 = null;

        for (var event : capturedPublishEvents_FAILED_CONNECT) {
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
        }
        assertNotNull(expectedEvent0);
        assertFalse(expectedEvent0.isConnected());

        var statusWrapper_FAILED_CONNECT = capturedLogEvent_FAILED_CONNECT.get(0);
        assertEquals(MARKER_CRITICAL_INFORMATION, statusWrapper_FAILED_CONNECT.getMarker());
        assertEquals(MESSAGE_NFC_READER_CONNECT_FAILURE, statusWrapper_FAILED_CONNECT.getMessage());
        assertNull(statusWrapper_FAILED_CONNECT.getDeviceId());
        assertEquals("0x000000FF", statusWrapper_FAILED_CONNECT.getStatus());
        //</editor-fold>
    }
}
