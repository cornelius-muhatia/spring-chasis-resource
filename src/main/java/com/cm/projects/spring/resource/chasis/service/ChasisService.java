package com.cm.projects.spring.resource.chasis.service;

import com.cm.projects.spring.resource.chasis.annotations.Unique;
import com.cm.projects.spring.resource.chasis.exceptions.ExpectationFailed;
import com.cm.projects.spring.resource.chasis.exceptions.GeneralBadRequest;
import com.cm.projects.spring.resource.chasis.exceptions.NotFoundException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessor;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

/**
 * Handles chasis business logic such as validations and persisting entities. Methods include:
 * <ul>
 *     <li>{@link ChasisService#validateRelEntities(Object, PropertyAccessor)}</li>
 * </ul>
 */
public interface ChasisService {

    /**
     * Checks if relation entities exists in the repository
     * @param t Entity
     * @param accessor Bean Wrapper
     * @throws NotFoundException if relational entities cannot be found
     * @throws ExpectationFailed if relational entities are not active
     */
    void validateRelEntities(Object t, PropertyAccessor accessor) throws NotFoundException, ExpectationFailed;

    /**
     * Used to validate unique fields in a given entity against existing records
     * in the database. Unique fields are identified using @{@link Unique}
     * annotation.
     *
     * @param accessor entity bean wrapper
     * @throws RuntimeException If the current field doesn't have an id field
     * (Field annotated with @{@link Id})
     * @throws GeneralBadRequest If unique validation fails on a field annotated
     * with @{@link Unique} annotation
     */
    void validateUniqueFields(BeanWrapper accessor) throws GeneralBadRequest;

    /**
     * Validate unique fields in a collections
     *
     * @param t
     * @throws GeneralBadRequest
     * @see ChasisService#validateUniqueFields(BeanWrapper)
     */
    void validateUniqueFields(Collection<?> t) throws GeneralBadRequest;

    /**
     * Instatiate an entity with an id.
     * @param id entity id
     * @return Entity Object and {@link null} if the id field cannot be found
     * @throws NoSuchMethodException if get field cannot be found for the id field
     * @throws IllegalAccessException if the constructor method is inaccessible
     * @throws InvocationTargetException if number of constructor args differ
     * @throws InstantiationException if the constructor represents an abstract class
     */
    <T> Object instantiateEntity(T id, Class<?> type) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException;

    /**
     * Used to retrieve id from an entity
     * @param entity Entity record
     * @return {@link Serializable}
     */
    Serializable getEntityId(Object entity);

    /**
     * Parse date
     * @param dateString date as a string
     * @return {@link Date} can return null if can't parse Date
     * @throws ParseException if string date cannot be parsed
     */
    Date tryParse(String dateString)throws ParseException;

    /**
     * Used to stream records from the persistance unit using the specified filters
     * @param pg pagealbe object containing size and sort params
     * @param clazz Entity class
     * @param request {@link HttpServletRequest}
     * @param entityManager {@link EntityManager}
     * @param <T> Entity Class Type
     * @return {@link Stream} of entities
     * @throws ParseException Occurs when date cannot be parsed to {@link Date}
     */
    <T> Stream<T> findAll(Pageable pg, Class<T> clazz, HttpServletRequest request, EntityManager entityManager) throws ParseException;
}
