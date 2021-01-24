package com.howellsmith.oss.nfcpipass.config;

import com.howellsmith.oss.nfcpipass.ufr.UfrDevice;
import com.sun.jna.Native;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.howellsmith.oss.nfcpipass.config.StringConstants.MESSAGE_NATIVE_LIBRARY_LOAD_FAILURE;
import static com.howellsmith.oss.nfcpipass.ufr.Utilities.NATIVE_LIBRARY_ABSOLUTE_PATH;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * uFR Bean Creation and Configuration
 */
@Log4j2
@EnableScheduling
@Configuration
public class UfrBeanConfig {

    @Bean
    public UfrDevice loadLibrary() {
        var lib = NATIVE_LIBRARY_ABSOLUTE_PATH.get();
        try {
            log.info("Loading Native Library: {}", lib);
            return Native.load(lib, UfrDevice.class);
        } catch (Exception e) {
            log.fatal("Failed to load native library: {}, {}; {}", lib, getMessage(e), getStackTrace(e));
            throw new BeanCreationException(MESSAGE_NATIVE_LIBRARY_LOAD_FAILURE);
        }
    }
}
