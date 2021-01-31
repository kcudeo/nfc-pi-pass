package com.howellsmith.oss.nfcpipass.service;

import com.howellsmith.oss.nfcpipass.model.event.DeviceConnectionStatusEvent;
import com.howellsmith.oss.nfcpipass.model.event.TagInFieldEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class EventLogger {

    private final UnifiedLoggingService log;

    public EventLogger(UnifiedLoggingService log) {
        this.log = log;
    }

    @EventListener
    public void logDeviceConnectionStatusEvent(DeviceConnectionStatusEvent event) {
        log.info(event);
    }

    @EventListener
    public void logTagInFieldEvent(TagInFieldEvent event) {
        log.info(event);
    }
}
