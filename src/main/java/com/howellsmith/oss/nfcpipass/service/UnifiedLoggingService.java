package com.howellsmith.oss.nfcpipass.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
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
 * search JSON log message that seamlessly integrates
 *
 * @author Taylor Howellsmith
 */
@Log4j2
@Service
public class UnifiedLoggingService {

    private static final String CLASS = "_class";
    private static final String LINE = "_lineNumber";

    private final ObjectMapper mapper;

    public UnifiedLoggingService(@Qualifier("forgivingMapper") ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    public void info(Map<String, Object> objectMap) {
        writeLog(INFO, objectMap);
    }

    public void warn(Map<String, Object> objectMap) {
        writeLog(WARN, objectMap);
    }

    public void error(Map<String, Object> objectMap) {
        writeLog(ERROR, objectMap);
    }

    public void debug(Map<String, Object> objectMap) {
        writeLog(DEBUG, objectMap);
    }

    public void trace(Map<String, Object> objectMap) {
        writeLog(TRACE, objectMap);
    }

    public void fatal(Map<String, Object> objectMap) {
        writeLog(FATAL, objectMap);
    }

    private void writeLog(StandardLevel level, Map<String, Object> objectMap) {

        if (Objects.isNull(objectMap)) {
            throw new IllegalArgumentException(MESSAGE_ILLEGAL_ARGUMENT_NULL);
        }

        objectMap.putAll(getSource());
        switch (level) {
            case INFO:
                log.info(format(objectMap));
                break;
            case WARN:
                log.warn(format(objectMap));
                break;
            case ERROR:
                log.error(format(objectMap));
                break;
            case DEBUG:
                log.debug(format(objectMap));
                break;
            case TRACE:
                log.trace(format(objectMap));
                break;
            case FATAL:
                log.fatal(format(objectMap));
                break;
        }
    }

    /**
     * Pull the source of the original call from the stack and get the class and line number.
     *
     * @return Returns a list of Pairs that contain the calling class name and line number.
     */
    private Map<String, Object> getSource() {
        var clazz = Thread.currentThread().getStackTrace()[5].getClassName();
        var lineNumber = Thread.currentThread().getStackTrace()[5].getLineNumber();
        return Map.of(CLASS, clazz, LINE, lineNumber);
    }

    /**
     * Private utility method to transform the object map into a string.
     */
    private String format(Map<String, Object> objectMap) {
        try {
            return mapper.writeValueAsString(objectMap);
        } catch (JsonProcessingException e) {
            return objectMap.toString();
        }
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
}
