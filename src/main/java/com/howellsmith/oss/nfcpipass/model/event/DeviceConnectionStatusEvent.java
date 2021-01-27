package com.howellsmith.oss.nfcpipass.model.event;

import org.springframework.context.ApplicationEvent;

/**
 * Simple event to indicate the connected status of the NFC hardware.
 */
public class DeviceConnectionStatusEvent extends ApplicationEvent {

    private boolean isConnected;

    public DeviceConnectionStatusEvent(Object source, boolean isConnected) {
        super(source);
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }
}
