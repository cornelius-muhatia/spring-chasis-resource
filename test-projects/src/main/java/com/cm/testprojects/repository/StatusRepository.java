package com.cm.testprojects.repository;

import com.cm.testprojects.entities.Status;
import org.springframework.data.repository.CrudRepository;

public interface StatusRepository extends CrudRepository<Status, Short> {
}
