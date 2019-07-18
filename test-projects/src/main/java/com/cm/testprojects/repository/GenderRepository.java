package com.cm.testprojects.repository;

import com.cm.testprojects.entities.Gender;
import org.springframework.data.repository.CrudRepository;

public interface GenderRepository extends CrudRepository<Gender, Short> {
}
