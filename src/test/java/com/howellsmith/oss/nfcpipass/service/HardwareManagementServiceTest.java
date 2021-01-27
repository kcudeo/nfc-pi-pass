package com.howellsmith.oss.nfcpipass.service;

import com.howellsmith.oss.nfcpipass.model.event.DeviceConnectionStatusEvent;
import com.howellsmith.oss.nfcpipass.model.event.TagInFieldEvent;
import com.howellsmith.oss.nfcpipass.ufr.UfrDevice;
import com.sun.jna.ptr.ByteByReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import javax.swing.text.TabableView;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.IntStream;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.MARKER_CRITICAL_INFORMATION;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NFC_READER_CONNECT_SUCCESS;
import static com.howellsmith.oss.nfcpipass.ufr.ErrorCodes.DL_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

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
    public void test_initial_connection() throws InvocationTargetException, IllegalAccessException {

        Mockito.when(device.ReaderOpen()).thenReturn(DL_OK);
        Mockito.doAnswer((Answer<Integer>) invocation -> {

            var cardId = invocation.getArgument(0, ByteByReference.class);
            byte[] uid = invocation.getArgument(1);
            var uidSize = invocation.getArgument(2, ByteByReference.class);

            cardId.setValue((byte) 0xFF);
            for(int i = 0; i < 8; i++){
                uid[i] = (byte) i;
            }
            uidSize.setValue((byte) 0x08);

            return DL_OK;
        }).when(device).GetCardIdEx(any(), any(), any());

        Mockito.doAnswer((Answer<Integer>) invocation -> {

            byte[] uid = invocation.getArgument(0);
            var text = "TESTING".getBytes();

            System.arraycopy(text, 0, uid, 0, text.length);

            return DL_OK;
        }).when(device).ReadNdefRecord_Text(any());

        var publishCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        var logCaptor = ArgumentCaptor
                .forClass(HardwareManagementService.DeviceStatusLogWrapper.class);

        poll.invoke(service);

        Mockito.verify(publisher, times(2)).publishEvent(publishCaptor.capture());
        Mockito.verify(log, times(1)).info(logCaptor.capture());

        var capturedPublishEvents = publishCaptor.getAllValues();
        var capturedLogEvents = logCaptor.getAllValues();

        DeviceConnectionStatusEvent expectedEvent0 = null;
        TagInFieldEvent expectedEvent1 = null;

        for(var event: capturedPublishEvents){
            var clazz = event.getClass();
            switch (clazz.getSimpleName()) {
                case "DeviceConnectionStatusEvent" -> expectedEvent0 = (DeviceConnectionStatusEvent) event;
                case "TagInFieldEvent" -> expectedEvent1 = (TagInFieldEvent) event;
            }
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

        Mockito.verify(device, times(1)).ReaderOpen();
        Mockito.verify(device, times(1)).GetCardIdEx(any(), any(), any());
        Mockito.verify(device, times(1)).ReadNdefRecord_Text(any());
    }
}
