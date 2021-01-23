package com.howellsmith.oss.nfcpipass.dao;

import com.howellsmith.oss.nfcpipass.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Simple User Data Access Object
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    User findByRegisteredTagContaining(String tag);

    User findByCellPhone(String cellPhone);

    User findByEmail(String email);
}
