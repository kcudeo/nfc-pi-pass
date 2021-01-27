package com.howellsmith.oss.nfcpipass.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * The User representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;
    private List<String> registeredTag;
    private String firstName;
    private String lastName;
    private String cellPhone;
    private String email;

}

