package com.howellsmith.oss.nfcpipass.model.log.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.EXCEPTION_MSG_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.EXCEPTION_OBJECT_REFERENCE;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.EXCEPTION_TRACE_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.LOG_MARKER_KEY;
import static com.howellsmith.oss.nfcpipass.config.StringConstants.LOG_MSG_KEY;

@Builder
@Data
public class ExceptionalErrorLog {

    @JsonProperty(LOG_MARKER_KEY)
    private String marker;

    @JsonProperty(LOG_MSG_KEY)
    private String message;

    @JsonProperty(EXCEPTION_MSG_KEY)
    private String exceptionMessage;

    @JsonProperty(EXCEPTION_TRACE_KEY)
    private Map<Integer, String> stackTrace;

    @JsonProperty(EXCEPTION_OBJECT_REFERENCE)
    private Object reference;
}
