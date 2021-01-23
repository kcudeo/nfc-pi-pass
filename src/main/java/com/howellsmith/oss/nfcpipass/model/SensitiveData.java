package com.howellsmith.oss.nfcpipass.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

/**
 * The SensitiveData object holds things like passwords, notes or other data that is to be encrypted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveData {

    @Id
    private String id;

    /**
     * The tag id is associated with a {@link User}. Each tag holds the password to the private key that allows
     * decryption of the sensitive data in this object. Not every tag registered to a user can unlock all sensitive data
     * records.
     */
    private String tagId;

    /**
     * The data property is an encrypted string that can hold whatever data the user intends to have typed.
     */
    private String data;
}
