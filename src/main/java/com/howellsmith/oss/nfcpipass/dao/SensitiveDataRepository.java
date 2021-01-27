package com.howellsmith.oss.nfcpipass.dao;

import com.howellsmith.oss.nfcpipass.model.domain.SensitiveData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Simple SensitiveData Data Access Object
 */
@Repository
public interface SensitiveDataRepository extends MongoRepository<SensitiveData, String> {

    SensitiveData findByTagId(String tagId);

}
