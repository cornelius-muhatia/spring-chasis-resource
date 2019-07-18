package com.cm.testprojects.repository;

import com.cm.testprojects.entities.User;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Cornelius M.
 * @version 1.0.0
 */
public interface UserRepository extends CrudRepository<User, Long> {
}
