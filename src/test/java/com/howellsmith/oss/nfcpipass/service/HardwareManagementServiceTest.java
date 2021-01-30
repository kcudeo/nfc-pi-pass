package com.howellsmith.oss.nfcpipass.service;

import com.howellsmith.oss.nfcpipass.model.event.DeviceConnectionStatusEvent;
import com.howellsmith.oss.nfcpipass.model.event.TagInFieldEvent;
import com.howellsmith.oss.nfcpipass.ufr.UfrDevice;
import com.sun.jna.ptr.ByteByReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_CRITICAL_INFORMATION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_UNEXPECTED_READER_STATUS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_GET_READER_TYPE_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_FAILURE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_SUCCESS;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_UNEXPECTED_READER_STATUS;
import static com.howellsmith.oss.nfcpipass.service.HardwareManagementService.DeviceStatusLogWrapper;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.DL_OK;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.NO_CARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class HardwareManagementServiceTest {

    private static UnifiedLoggingService log;
    private static UfrDevice device;
    private static ApplicationEventPublisher publisher;
    private static HardwareManagementService service;
    private static Method poll;

    private static final String DCSE = "DeviceConnectionStatusEvent";
    private static final String TIFE = "TagInFieldEvent";
    private static final Answer<Integer> GET_CARD_ID_EX_ANSWER = i -> {
        i.getArgument(0, ByteByReference.class).setValue((byte) 0xFF);
        i.getArgument(2, ByteByReference.class).setValue((byte) 0x08);
        for (int b = 0; b < 8; b++) i.getArgument(1, byte[].class)[b] = (byte) b;
        return DL_OK;
    };

    private final List<ApplicationEvent> trashEventList = new ArrayList<>();
    private final List<DeviceStatusLogWrapper> trashLogEvents = new ArrayList<>();

    @BeforeAll
    void setup() throws NoSuchMethodException {

        // define mocks
        log = mock(UnifiedLoggingService.class);
        device = mock(UfrDevice.class);
        publisher = mock(ApplicationEventPublisher.class);

        // setup service
        service = new HardwareManagementService(log, device, publisher);

        // enable access
        poll = service.getClass().getDeclaredMethod("poll");
        poll.setAccessible(true);

        // define default handling
        doNothing().when(publisher).publishEvent(any());
        doNothing().when(log).info(any());
        doNothing().when(log).error(any());
    }

    @BeforeEach
    void before_each() {
        reset(device);
    }

    @Test
    @Order(1)
    void test_connect_then_valid_card() throws InvocationTargetException, IllegalAccessException {

        when(device.ReaderOpen())
                .thenReturn(DL_OK);
        doAnswer(GET_CARD_ID_EX_ANSWER)
                .when(device)
                .GetCardIdEx(any(), any(), any());
        doAnswer((Answer<Integer>) i -> {
            System.arraycopy("TESTING".getBytes(), 0, i.getArgument(0, byte[].class), 0, 7);
            return DL_OK;
        }).when(device).ReadNdefRecord_Text(any());

        // Setup argument captors for log and event validation
        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor.forClass(DeviceStatusLogWrapper.class);

        // Emulate the poll for the first time
        poll.invoke(service);

        // Install the captors
        verify(publisher, times(2))
                .publishEvent(publishCaptor.capture());
        verify(log, times(1))
                .info(logCaptor.capture());

        // Get the data
        var capturedPublishEvents = publishCaptor.getAllValues();
        trashEventList.addAll(capturedPublishEvents);
        var capturedLogEvents = logCaptor.getAllValues();
        trashLogEvents.addAll(capturedLogEvents);

        DeviceConnectionStatusEvent expectedEvent0 = null;
        TagInFieldEvent expectedEvent1 = null;

        for (var event : capturedPublishEvents)
            switch (event.getClass().getSimpleName()) {
                case DCSE -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case TIFE -> expectedEvent1 = (TagInFieldEvent) event;
            }

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

        verify(device, times(1))
                .ReaderOpen();
        verify(device, times(1))
                .GetCardIdEx(any(), any(), any());
        verify(device, times(1))
                .ReadNdefRecord_Text(any());
    }

    @Test
    @Order(2)
    void test_connect_then_invalid_card() throws InvocationTargetException, IllegalAccessException {

        when(device.ReadNdefRecord_Text(any()))
                .thenReturn(0xFF);
        doAnswer(GET_CARD_ID_EX_ANSWER)
                .when(device)
                .GetCardIdEx(any(), any(), any());
        doAnswer((Answer<Integer>) i -> {
            System.arraycopy(new int[]{1, 1}, 0, i.getArgument(0, int[].class), 0, 2);
            return DL_OK;
        }).when(device).GetReaderType(any(int[].class));

        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor.forClass(DeviceStatusLogWrapper.class);

        poll.invoke(service);

        verify(publisher, times(4))
                .publishEvent(publishCaptor.capture());
        verify(log, times(1))
                .error(logCaptor.capture());

        var capturedPublishEvents = publishCaptor.getAllValues();
        capturedPublishEvents.removeAll(trashEventList);
        trashEventList.addAll(capturedPublishEvents);

        var capturedLogEvent = logCaptor.getAllValues();
        capturedLogEvent.removeAll(trashLogEvents);
        trashLogEvents.addAll(capturedLogEvent);

        assertEquals(1, capturedLogEvent.size());
        assertEquals(2, capturedPublishEvents.size());

        DeviceConnectionStatusEvent expectedEvent0 = null;
        TagInFieldEvent expectedEvent1 = null;

        for (var event : capturedPublishEvents)
            switch (event.getClass().getSimpleName()) {
                case DCSE -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case TIFE -> expectedEvent1 = (TagInFieldEvent) event;
            }

        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertTrue(expectedEvent1.isDeviceInField());
        assertEquals(0x0706050403020100L, expectedEvent1.getUid());
        assertNull(expectedEvent1.getNdef());
    }

    @Test
    @Order(3)
    void test_connect_then_no_card() throws InvocationTargetException, IllegalAccessException {

        when(device.GetCardIdEx(any(), any(), any()))
                .thenReturn(NO_CARD);
        doAnswer((Answer<Integer>) i -> {
            System.arraycopy(new int[]{1, 1}, 0, i.getArgument(0, int[].class), 0, 2);
            return DL_OK;
        }).when(device).GetReaderType(any(int[].class));

        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor.forClass(DeviceStatusLogWrapper.class);

        poll.invoke(service);

        verify(publisher, times(6))
                .publishEvent(publishCaptor.capture());
        verify(log, times(1))
                .info(logCaptor.capture());

        var capturedPublishEvents = publishCaptor.getAllValues();
        capturedPublishEvents.removeAll(trashEventList);
        trashEventList.addAll(capturedPublishEvents);

        var capturedLogEvent = logCaptor.getAllValues();
        capturedLogEvent.removeAll(trashLogEvents);
        trashLogEvents.addAll(capturedLogEvent);

        assertEquals(0, capturedLogEvent.size());
        assertEquals(2, capturedPublishEvents.size());

        DeviceConnectionStatusEvent expectedEvent0 = null;
        TagInFieldEvent expectedEvent1 = null;

        for (var event : capturedPublishEvents)
            switch (event.getClass().getSimpleName()) {
                case DCSE -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case TIFE -> expectedEvent1 = (TagInFieldEvent) event;
            }

        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertFalse(expectedEvent1.isDeviceInField());
        assertNull(expectedEvent1.getUid());
        assertNull(expectedEvent1.getNdef());
    }

    @Test
    @Order(4)
    void test_connect_then_unknown_status() throws InvocationTargetException, IllegalAccessException {

        when(device.GetCardIdEx(any(), any(), any()))
                .thenReturn(0xFF);
        doAnswer((Answer<Integer>) i -> {
            System.arraycopy(new int[]{1, 1}, 0, i.getArgument(0, int[].class), 0, 2);
            return DL_OK;
        }).when(device).GetReaderType(any(int[].class));

        // Setup argument captors for log and event validation
        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor.forClass(DeviceStatusLogWrapper.class);

        poll.invoke(service);

        verify(publisher, times(8))
                .publishEvent(publishCaptor.capture());
        verify(log, times(2))
                .error(logCaptor.capture());

        var capturedPublishEvents = publishCaptor.getAllValues();
        capturedPublishEvents.removeAll(trashEventList);
        trashEventList.addAll(capturedPublishEvents);

        var capturedLogEvent = logCaptor.getAllValues();
        capturedLogEvent.removeAll(trashLogEvents);
        trashLogEvents.addAll(capturedLogEvent);

        assertEquals(1, capturedLogEvent.size());
        assertEquals(2, capturedPublishEvents.size());

        DeviceConnectionStatusEvent expectedEvent0 = null;
        TagInFieldEvent expectedEvent1 = null;

        for (var event : capturedPublishEvents)
            switch (event.getClass().getSimpleName()) {
                case DCSE -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case TIFE -> expectedEvent1 = (TagInFieldEvent) event;
            }

        assertNotNull(expectedEvent0);
        assertTrue(expectedEvent0.isConnected());

        assertNotNull(expectedEvent1);
        assertFalse(expectedEvent1.isDeviceInField());
        assertNull(expectedEvent1.getUid());
        assertNull(expectedEvent1.getNdef());

        var statusWrapper = capturedLogEvent.get(0);
        assertEquals(MARKER_UNEXPECTED_READER_STATUS, statusWrapper.getMarker());
        assertEquals(MESSAGE_UNEXPECTED_READER_STATUS, statusWrapper.getMessage());
        assertEquals("0x00000001", statusWrapper.getDeviceId());
        assertEquals("0x000000FF", statusWrapper.getStatus());
    }

    @Test
    @Order(5)
    void test_previous_connect_success_to_no_device() throws InvocationTargetException, IllegalAccessException {

        when(device.GetReaderType(any(int[].class))).thenReturn(0xFF);

        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor.forClass(DeviceStatusLogWrapper.class);

        poll.invoke(service);

        verify(publisher, times(9))
                .publishEvent(publishCaptor.capture());
        verify(log, times(3))
                .error(logCaptor.capture());

        var capturedPublishEvents = publishCaptor.getAllValues();
        capturedPublishEvents.removeAll(trashEventList);
        trashEventList.addAll(capturedPublishEvents);

        var capturedLogEvent = logCaptor.getAllValues();
        capturedLogEvent.removeAll(trashLogEvents);
        trashLogEvents.addAll(capturedLogEvent);

        assertEquals(1, capturedLogEvent.size());
        assertEquals(1, capturedPublishEvents.size());

        var expectedEvent0 = (DeviceConnectionStatusEvent) capturedPublishEvents.get(0);
        assertNotNull(expectedEvent0);
        assertFalse(expectedEvent0.isConnected());

        var statusWrapper = capturedLogEvent.get(0);
        assertEquals(MARKER_CRITICAL_INFORMATION, statusWrapper.getMarker());
        assertEquals(MESSAGE_NFC_GET_READER_TYPE_FAILURE, statusWrapper.getMessage());
        assertEquals("0x00000001", statusWrapper.getDeviceId());
        assertEquals("0x000000FF", statusWrapper.getStatus());
    }

    @Test
    @Order(6)
    void test_connect_fail() throws InvocationTargetException, IllegalAccessException {

        when(device.ReaderOpen()).thenReturn(0xFF);

        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor.forClass(DeviceStatusLogWrapper.class);

        poll.invoke(service);

        verify(publisher, times(10))
                .publishEvent(publishCaptor.capture());
        verify(log, times(4))
                .error(logCaptor.capture());

        var capturedPublishEvents = publishCaptor.getAllValues();
        capturedPublishEvents.removeAll(trashEventList);
        trashEventList.addAll(capturedPublishEvents);

        var capturedLogEvent = logCaptor.getAllValues();
        capturedLogEvent.removeAll(trashLogEvents);
        trashLogEvents.addAll(capturedLogEvent);

        assertEquals(1, capturedLogEvent.size());
        assertEquals(1, capturedPublishEvents.size());

        var expectedEvent0 = (DeviceConnectionStatusEvent) capturedPublishEvents.get(0);
        assertNotNull(expectedEvent0);
        assertFalse(expectedEvent0.isConnected());

        var statusWrapper = capturedLogEvent.get(0);
        assertEquals(MARKER_CRITICAL_INFORMATION, statusWrapper.getMarker());
        assertEquals(MESSAGE_NFC_READER_CONNECT_FAILURE, statusWrapper.getMessage());
        assertNull(statusWrapper.getDeviceId());
        assertEquals("0x000000FF", statusWrapper.getStatus());
    }
}
