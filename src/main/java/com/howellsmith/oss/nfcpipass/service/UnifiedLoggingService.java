package com.howellsmith.oss.nfcpipass.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.spi.StandardLevel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_ILLEGAL_ARGUMENT_NULL;
import static org.apache.logging.log4j.spi.StandardLevel.DEBUG;
import static org.apache.logging.log4j.spi.StandardLevel.ERROR;
import static org.apache.logging.log4j.spi.StandardLevel.FATAL;
import static org.apache.logging.log4j.spi.StandardLevel.INFO;
import static org.apache.logging.log4j.spi.StandardLevel.TRACE;
import static org.apache.logging.log4j.spi.StandardLevel.WARN;

/**
 * The Unified Logging Service is designed to accept a series of arguments that can be translated into an easy to
 * search JSON log message that seamlessly integrates with log ingestion systems.
 *
 * @author Taylor Howellsmith
 */
@Log4j2
@Service
public class UnifiedLoggingService {

    private static final String CLASS = "_class";
    private static final String LINE = "_lineNumber";
    private static final String METHOD = "_method";

    private final ObjectMapper mapper;

    public UnifiedLoggingService(@Qualifier("forgivingMapper") ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    public void info(Object o) {
        writeLog(INFO, o);
    }

    public void warn(Object o) {
        writeLog(WARN, o);
    }

    public void error(Object o) {
        writeLog(ERROR, o);
    }

    public void debug(Object o) {
        writeLog(DEBUG, o);
    }

    public void trace(Object o) {
        writeLog(TRACE, o);
    }

    public void fatal(Object o) {
        writeLog(FATAL, o);
    }

    private void writeLog(StandardLevel level, Object o) {

        if (Objects.isNull(o))
            throw new IllegalArgumentException(MESSAGE_ILLEGAL_ARGUMENT_NULL);

        var wrapped = MessageWrapper.builder()
                .source(getSource())
                .data(o)
                .build();

        switch (level) {
            case INFO:
                log.info(new ObjectMessage(wrapped));
                break;
            case WARN:
                log.warn(new ObjectMessage(wrapped));
                break;
            case ERROR:
                log.error(new ObjectMessage(wrapped));
                break;
            case DEBUG:
                log.debug(new ObjectMessage(wrapped));
                break;
            case TRACE:
                log.trace(new ObjectMessage(wrapped));
                break;
            case FATAL:
                log.fatal(new ObjectMessage(wrapped));
                break;
        }
    }

    /**
     * Pull the source of the original call from the stack and get the class and line number.
     *
     * @return Returns a list of Pairs that contain the calling class name and line number.
     */
    private Map<String, Object> getSource() {
        var clazz = Thread.currentThread().getStackTrace()[4].getClassName();
        var lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
        var method = Thread.currentThread().getStackTrace()[4].getMethodName();
        return Map.of(CLASS, clazz, LINE, lineNumber, METHOD, method);
    }

    /**
     * Returns an ordered map of the stacktrace associated with the provided throwable. This is useful to add to a log
     * message in the case of an unexpected error.
     *
     * @param throwable The exception encountered.
     * @return Returns an Integer -> String Map that contains one entry per line in the stack trace of the exception
     * provided.
     */
    public static Map<Integer, String> getStackMap(Throwable throwable) {
        var elements = throwable.getStackTrace();
        return IntStream.range(0, elements.length)
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> elements[i].toString()));
    }

    /**
     * Simple wrapper class to hold the source information and the passed in object.
     */
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class MessageWrapper {

        private static final String SOURCE = "_source";
        private static final String DATA = "_data";

        @JsonProperty(SOURCE)
        private Map<String, Object> source;

        @JsonProperty(DATA)
        private Object data;

    }
}
