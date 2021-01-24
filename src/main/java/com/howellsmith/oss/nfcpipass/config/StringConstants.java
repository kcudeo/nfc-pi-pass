package com.howellsmith.oss.nfcpipass.config;

/**
 * A class used to hold string constants.
 */
public class StringConstants {

    /**
     * Used to denote an appropriate marker for the log message.
     */
    public static final String LOG_MARKER_KEY = "_marker";
    public static final String LOG_MSG_KEY = "_msg";
    public static final String EXCEPTION_MSG_KEY = "_exception_msg";
    public static final String EXCEPTION_CLASS_KEY = "_exception_class";
    public static final String EXCEPTION_LINE_KEY = "_exception_line";
    public static final String EXCEPTION_TRACE_KEY = "_exception_stack_trace";
    public static final String EXCEPTION_OBJECT_REFERENCE = "_exception_object_reference";

    public static final String DEVICE_STATUS = "deviceStatus";
    public static final String DEVICE_ID = "deviceId";

    public static final String MARKER_CRITICAL_INFORMATION = "CRITICAL_INFORMATION_MARKER";
    public static final String MARKER_ILLEGAL_ARGUMENT_NULL = "ILLEGAL_ARGUMENT_NULL_MARKER";
    public static final String MARKER_UNEXPECTED_EXCEPTION = "MARKER_UNEXPECTED_EXCEPTION";

    public static final String MESSAGE_ILLEGAL_ARGUMENT_NULL = "The supplied argument cannot be null.";
    public static final String MESSAGE_NATIVE_LIBRARY_LOAD_FAILURE = "Failed to load the required native library.";
    public static final String MESSAGE_NFC_GET_READER_TYPE_FAILURE = "Failed to get the device reader type information.";
    public static final String MESSAGE_NFC_READER_CONNECT_SUCCESS = "Successfully connected the uFR NFC reader.";
    public static final String MESSAGE_NFC_READER_CONNECT_FAILURE = "Unable to connect to the uFR NFC reader.";
    public static final String MESSAGE_SLEEP_INTERRUPTED_DURING_RESET = "Thread sleep interrupted during device reset waiting period.";

}
