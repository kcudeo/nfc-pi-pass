package com.howellsmith.oss.nfcpipass.model.event;

import org.springframework.context.ApplicationEvent;

/**
 * An event object that represents an NFC tag currently in the field.
 */
public class TagInFieldEvent extends ApplicationEvent {

    private boolean inField;
    private Long uid;
    private String ndef;

    public TagInFieldEvent(Object source, boolean inField, Long uid, String ndef) {
        super(source);
        this.inField = inField;
        this.uid = uid;
        this.ndef = ndef;
    }

    public boolean isDeviceInField(){
        return this.inField;
    }

    public Long getUid() {
        return this.uid;
    }

    public String getNdef() {
        return this.ndef;
    }
}
