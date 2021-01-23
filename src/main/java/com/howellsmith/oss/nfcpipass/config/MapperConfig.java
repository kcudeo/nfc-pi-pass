package com.howellsmith.oss.nfcpipass.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Configuration
public class MapperConfig {

    @Primary
    @Bean("forgivingMapper")
    public ObjectMapper getForgivingMapper(){
        return new ObjectMapper()
                .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
