/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cm.projects.spring.resource.chasis.repository;

import com.cm.projects.spring.resource.chasis.annotations.*;
import com.cm.projects.spring.resource.chasis.exceptions.ExpectationFailed;
import com.cm.projects.spring.resource.chasis.utils.AppConstants;
import com.cm.projects.spring.resource.chasis.utils.SharedMethods;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessorFactory;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

//import javax.persistence.PersistenceContext;
//import org.springframework.stereotype.Component;

/**
 * Used to handle update requests (persisting changes in the edited entity, fetching changes,
 * declining changes and updating entity with the changes from edited entity)
 *
 * @param <T> entity affected by the changes
 * @param <E> edited entity used to store changes temporarily before approval
 * @author Cornelius M
 * @version 0.0.1
 */
public class SupportRepository<T, E> {

    /**
     * Used to handle logging mainly to the console but you can implement an appender for more options
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    /**
     * Entity mapping
     */
    private final Class<T> entityClazz;
    /**
     * Edited entity mapping
     */
    private final Class<E> editedEnClazz;
    /**
     * Entity manager used to handle persisting requests
     */
    private final EntityManager entityManager;
    /**
     * Criteria builder used to process HQL queries
     */
    private final CriteriaBuilder builder;

    /**
     * Used to instantiate the class
     *
     * @param entityManager   entity manager to handle transactions
     * @param entityMapping   entity mapping
     * @param editedEnMapping edited entity mapping
     */
    public SupportRepository(EntityManager entityManager, Class<T> entityMapping, Class<E> editedEnMapping) {
        this.entityManager = entityManager;
        this.builder = entityManager.getCriteriaBuilder();
        this.entityClazz = entityMapping;
        this.editedEnClazz = editedEnMapping;
    }

    /**
     * Checks if changes were made if true it persists changes to entity
     * storage. This method is different from super class since changes don't
     * affect the original entity they are saved in the edit entity first
     *
     * @param entity     current entity
     * @param oldEntity  previous entity
     * @param editEntity entity storage
     * @return {@link List} of changes or an empty {@link List} if changes were
     * not found found
     * @throws java.lang.IllegalAccessException                   if it fails to locate Id field
     * @throws com.fasterxml.jackson.core.JsonProcessingException if it fails to
     *                                                            save the current entity to entity storage
     * @throws ExpectationFailed                                  When editEntity param does not have fields
     *                                                            annotated;
     *                                                            <ul>
     *                                                            <li>@{@link EditDataWrapper} used to store new edited data</li>
     *                                                            <li>@{@link EditEntityId} used to store the id of the entity being
     *                                                            updated</li>
     *                                                            <li>@{@link EditEntity} used to store name of entity being updated</li>
     *                                                            </ul>
     */
    public List<String> handleEditRequest(T entity, T oldEntity, Class<E> editEntity) throws
            IllegalAccessException, JsonProcessingException, ExpectationFailed {

        Serializable index = null;
        List<String> changes = new ArrayList<>();

        if (null != entity) {
            //Check if entity has been modified
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (field.isAnnotationPresent(Id.class)) {
                    index = (Serializable) field.get(entity);
                }
            }
            if (index == null) {
                log.warn("Failed to find id field on entity {} during handling edit request", entity);
            } else {
                changes = this.fetchChanges(entity, oldEntity);
                //If there are changes, update this field
                if (!changes.isEmpty()) {
                    BeanWrapper wrapper = new BeanWrapperImpl(editEntity);
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.addMixIn(Object.class, DynamicMixIn.class);
                    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    //ignore entities
                    Set<String> ignoreProperties = new HashSet<>();
                    for (Field field : oldEntity.getClass().getDeclaredFields()) {
//                        if ((field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToOne.class)) &&
//                                !field.isAnnotationPresent(ModifiableField.class)) {
//                            ignoreProperties.add(field.getName());
//                        }else
                        if (!field.isAnnotationPresent(ModifiableField.class) && !field.isAnnotationPresent(Id.class)) {
                            ignoreProperties.add(field.getName());
                        }
                    }

                    FilterProvider filters = new SimpleFilterProvider()
                            .addFilter("dynamicFilter", SimpleBeanPropertyFilter.serializeAllExcept(ignoreProperties));
                    mapper.setFilterProvider(filters);

                    String data = mapper.writeValueAsString(entity);
                    CriteriaDelete c = this.builder.createCriteriaDelete(editEntity);
                    Root criteriaRoot = c.from(editEntity);
                    Predicate[] preds = new Predicate[2];
                    for (Field field : editEntity.getDeclaredFields()) {
                        if (field.isAnnotationPresent(EditEntity.class)) {
                            wrapper.setPropertyValue(field.getName(), entity.getClass().getSimpleName());
                            preds[0] = this.builder.equal(criteriaRoot.get(field.getName()), entity.getClass().getSimpleName());
                        } else if (field.isAnnotationPresent(EditDataWrapper.class)) {
                            wrapper.setPropertyValue(field.getName(), data);
                        } else if (field.isAnnotationPresent(EditEntityId.class)) {
                            if (field.getType().isAssignableFrom(String.class)) {
                                wrapper.setPropertyValue(field.getName(), index.toString());
                                preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), index.toString());
                            } else {
                                wrapper.setPropertyValue(field.getName(), index);
                                preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), index);
                            }

                        }
                    }

                    //delete existing records to avoid data redudancy
                    if (preds[0] != null && preds[1] != null) {
                        c.where(preds);
                        this.entityManager.createQuery(c).executeUpdate();
                    } else {
                        throw new ExpectationFailed("Failed to update edited edit "
                                + "due to either the fields annotated with @EditDataWrapper, @EditEntityId and @EditEntity could not be found");
                    }
                    E e = (E) wrapper.getWrappedInstance();
                    this.entityManager.persist(e);
                }
            }
        }

        return changes;
    }

    /**
     * Used to fetch {@link List} of changes between two entities. Only fields annotated with {@link ModifiableField} are compared
     * For {@link OneToMany} field, {@link ModifiableField#name()} is used for changes description. For Example <br />
     * <strong>Primary Class</strong>
     * <pre>
     *      class Role{
     *              <code>@Id</code>
     *              private Short id;
     *              <code>@ModifiableField(name = "Permission")</code>
     *              <code>@OneToMany</code>
     *              private <code>List<RolePermission> permissions</code>
     *      }
     * </pre>
     * <strong>Mapper Class</strong>
     * <pre>
     *     class RolePermission{
     *         <code>@Id</code>
     *         private Short id;
     *         <code>@RelEntityLabel(fieldName="permission")</code>
     *         <code>@ManyToOne</code>
     *         private Permission permission;
     *     }
     * </pre>
     * <strong>Child Class</strong>
     * <pre>
     *     class Permission{
     *         <code>@Id</code>
     *         private Short id;
     *         private String permission = "Can Create User";
     *     }
     * </pre>
     * Will be "Added <strong><i>permission Can Create User</i></strong>"
     *
     * @param newBean updated entity
     * @param oldBean old entity
     * @return a {@link List} of {@link String} changes
     * @throws IllegalAccessException if fields with changes are not accessible
     */
    public List<String> fetchChanges(T oldBean, T newBean) throws IllegalAccessException {
        List<String> changes = new ArrayList<>();
        if (newBean.getClass() != oldBean.getClass()) {
            log.error(AppConstants.AUDIT_LOG, "Failed to fetch changes for {} and {}. "
                    + "Beans are not of the same class", oldBean.getClass(), newBean.getClass());
            return changes;
        }

        final Field[] allFields = oldBean.getClass().getDeclaredFields();
        for (Field field : allFields) {
            //Manage the fields that we need only
            if (field.isAnnotationPresent(ModifiableField.class)) {
                //Enable access of this field 
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object _newValue = field.get(oldBean);
                Object _oldValue = field.get(newBean);

                if (field.isAnnotationPresent(OneToMany.class)) {//process collection changes
                    changes.addAll(this.processCollectionChanges(field, (Collection) _oldValue, (Collection) _newValue));
                } else if (_newValue != _oldValue) {
                    log.debug("Found changes on field {} processing changes", field.getName());
                    if ((_newValue != null && !_newValue.equals(_oldValue))
                            || (_oldValue != null && !_oldValue.equals(_newValue))) {
                        if (_oldValue == null) {
                            if (field.isAnnotationPresent(ManyToOne.class)) {
                                changes.add("Assigned new value to " + SharedMethods.splitCamelString(field.getName()));
                            } else {
                                changes.add("Assigned " + _newValue + " to " + SharedMethods.splitCamelString(field.getName()));
                            }
                        } else if (_newValue == null) {
                            if (field.isAnnotationPresent(ManyToOne.class)) {
                                changes.add("Removed " + SharedMethods.splitCamelString(field.getName()) + "(" + this.getFieldValue(_oldValue, RelEntityLabel.class, false) + ")");
                            } else {
                                changes.add("Removed " + SharedMethods.splitCamelString(field.getName()) + "(" + _oldValue + ")");
                            }
                        } else {
                            if (field.isAnnotationPresent(ManyToOne.class)) {
                                changes.add(SharedMethods.splitCamelString(field.getName())
                                        + " changed from " + this.getFieldValue(_oldValue, RelEntityLabel.class, false) + " to " + this.getFieldValue(_newValue, RelEntityLabel.class, true));

                            } else {
                                changes.add(SharedMethods.splitCamelString(field.getName())
                                        + " changed from " + _oldValue + " to " + _newValue);
                            }
                        }
                    }
                }
            }
        }
        return changes;
    }

    /**
     * Used to compare two collection if their equals
     *
     * @param col1 first collection
     * @param col2 second collection
     * @return true if equals and false if not
     */
    public boolean compareCollection(Collection<?> col1, Collection<?> col2) {
        for (Object col : col1) {
            if (col2.stream().filter(val2 -> val2 != null && val2.equals(col)).collect(Collectors.toList()).isEmpty()) {
                return false;
            }
        }

        for (Object col : col2) {
            if (col1.stream().filter(val2 -> val2 != null && val2.equals(col)).collect(Collectors.toList()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Used to process changes
     *
     * @param field     field with collection entities
     * @param _oldValue existing value in the database
     * @param _newValue new value
     * @return {@link List} of changes
     */
    public List<String> processCollectionChanges(Field field, Collection<?> _oldValue, Collection<?> _newValue) {
        List<String> changes = new ArrayList<>();
        //check for removed entities
        _oldValue.forEach(val -> {
            String fieldLabel = "";
            if (_newValue.stream().filter(val2 -> val2 != null && val2.equals(val)).collect(Collectors.toList()).isEmpty()) {
                for (Field f : val.getClass().getDeclaredFields()) {
                    if (f.isAnnotationPresent(RelEntityLabel.class)) {
                        String label = f.getAnnotation(RelEntityLabel.class).fieldName();
                        if (!label.isBlank()) {
                            try {
                                f.setAccessible(true);
                                Object fVal = f.get(val);
                                BeanWrapper fAccessor = PropertyAccessorFactory.forBeanPropertyAccess(fVal);
                                fieldLabel = "" + fAccessor.getPropertyValue(label);
                            } catch (Exception e) {
                                log.info("Failed to find label field {} skipping description", e);
                            }

                        }
                    }
                }
                changes.add("Removed " + field.getAnnotation(ModifiableField.class).name() + " " + fieldLabel);
            }
        });
        //check for added entities
        _newValue.forEach(val -> {
            String fieldLabel = "";
            if (_oldValue.stream().filter(val2 -> val2 != null && val2.equals(val)).collect(Collectors.toList()).isEmpty()) {
                for (Field f : val.getClass().getDeclaredFields()) {
                    if (f.isAnnotationPresent(RelEntityLabel.class)) {
                        String label = f.getAnnotation(RelEntityLabel.class).fieldName();
                        if (!label.isBlank()) {
                            try {
                                f.setAccessible(true);
                                Object fVal = f.get(val);
                                for (Field f2 : fVal.getClass().getDeclaredFields()) {
                                    if (f2.isAnnotationPresent(Id.class)) {
                                        f2.setAccessible(true);
                                        fVal = this.entityManager.find(fVal.getClass(), f2.get(fVal));
                                    }
                                }
                                BeanWrapper fAccessor = PropertyAccessorFactory.forBeanPropertyAccess(fVal);
                                fieldLabel = "" + fAccessor.getPropertyValue(label);
                            } catch (Exception e) {
                                log.info("Failed to find label field {} skipping description", e);
                            }

                        }
                    }
                }
                changes.add("Added " + field.getAnnotation(ModifiableField.class).name() + " " + fieldLabel);
            }
        });
        return changes;
    }

    /**
     * Fetch field value from an with the declared annotation
     *
     * @param entity           target entity/POJO
     * @param annotation       annotation class for example @RelEntityLabel
     * @param useEntityManager true if value is to be fetched from entity manager
     * @return {@link Object} field value or null if it can't locate the value
     */
    public Object getFieldValue(Object entity, Class<? extends java.lang.annotation.Annotation> annotation, boolean useEntityManager) {

        BeanWrapper accessor = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        String fieldName = null;
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (useEntityManager && f.isAnnotationPresent(Id.class)) {
                accessor = PropertyAccessorFactory.forBeanPropertyAccess(this.entityManager.find(entity.getClass(), accessor.getPropertyValue(f.getName())));
            }
            if (f.isAnnotationPresent(annotation)) {
                fieldName = f.getName();
            }
        }
        //if entity doesn't have annotation
        log.debug("Found rel field name {} annotation {} and clazz {}", fieldName, annotation, entity.getClass());
        return (fieldName == null) ? null : accessor.getPropertyValue(fieldName);
//        return null;
    }

    /**
     * Used to fetch {@link List} of changes
     *
     * @param id entity id
     * @param t  the old entity
     * @return a {@link List} of {@link String} changes
     * @throws java.lang.IllegalAccessException if fields with changes are not accessible
     * @throws java.io.IOException              if changes cannot be mapped back to entity
     * @see SupportRepository#fetchChanges(Object, Object)
     */
    public List<String> fetchChanges(Serializable id, T t) throws IllegalAccessException, IOException {
        List<String> changes = new ArrayList<>();

        if (t == null) {
            log.warn("Failed to find entity with id {} returning a List of empty changes", id);
            return changes;
        }

        CriteriaQuery<E> c = this.builder.createQuery(editedEnClazz);
        String dataField = null;
        Root criteriaRoot = c.from(editedEnClazz);
        Predicate[] preds = new Predicate[2];

        for (Field field : editedEnClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EditEntity.class)) {
                log.debug("Adding restriction for field {} value {}", field.getName(), this.entityClazz.getSimpleName());
                preds[0] = this.builder.equal(criteriaRoot.get(field.getName()), this.entityClazz.getSimpleName());
            } else if (field.isAnnotationPresent(EditEntityId.class)) {
                if (field.getType().isAssignableFrom(String.class)) {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id.toString());
                } else {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id);
                }

            } else if (field.isAnnotationPresent(EditDataWrapper.class)) {
                dataField = field.getName();
            }
        }

        c.where(preds);
        E e;
        try {
            e = this.entityManager.createQuery(c).getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            log.warn("Failed to find changes on returning a list of empty changes");
            return changes;
        }
        if (e == null) {
            log.warn("Failed to find changes on edited entity {} returning a list of empty changes", e);
            return changes;
        }
        String data;
        BeanWrapper wrapper = new BeanWrapperImpl(e);
        data = (String) wrapper.getPropertyValue(dataField);

        if (null != data) {
            //Serialize object
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
            log.debug("Done configuring object mapper");
            T newbean = mapper.readValue(data, this.entityClazz);
            return this.fetchChanges(newbean, t);
        }

        return changes;
    }

    /**
     * Fetch entity excluding entities in trash
     *
     * @param id entity id
     * @return persistent context entity
     */
    public T fetchEntity(Serializable id) {
        //get id field name
        String fieldId = null;
        boolean hasIntrash = false;
        for (Field field : entityClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                fieldId = field.getName();
            }
            if (field.getName().equalsIgnoreCase("intrash")) {
                hasIntrash = true;
            }
        }

        if (fieldId == null) {
            throw new RuntimeException("Entity doesn't have an id field");
        }

        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClazz);
        Root<T> root = criteriaQuery.from(entityClazz);
        if (hasIntrash) {
            criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(root.get(fieldId), id),
                    criteriaBuilder.equal(root.get("intrash"), AppConstants.NO)));
        } else {
            criteriaQuery.where(criteriaBuilder.equal(root.get(fieldId), id));
        }
        try {
            return this.entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            return null;
        }
    }

    /**
     * Used to merge entity from Storage. Updates new values from edited record
     * to the provided entity
     *
     * @param t  The old entity cannot be null
     * @param id Unique key id of this entity
     * @return T new merged changes or the current entity if changes could not
     * be found
     * @throws java.io.IOException              occurs when changes cannot be mapped back to
     *                                          entity
     * @throws java.lang.IllegalAccessException occurs when the field annotated
     *                                          with @{@link ModifiableField} is not accessible
     * @throws NullPointerException             if t is null
     */
    public T mergeChanges(Serializable id, T t) throws IOException, IllegalArgumentException, IllegalAccessException {

        String data, dataField = null;
        CriteriaQuery<E> c = this.builder.createQuery(editedEnClazz);
        Root criteriaRoot = c.from(editedEnClazz);
        Predicate[] preds = new Predicate[2];

        for (Field field : editedEnClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EditEntity.class)) {
                preds[0] = this.builder.equal(criteriaRoot.get(field.getName()), this.entityClazz.getSimpleName());
            } else if (field.isAnnotationPresent(EditEntityId.class)) {
                if (field.getType().isAssignableFrom(String.class)) {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id.toString());
                } else {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id);
                }

            } else if (field.isAnnotationPresent(EditDataWrapper.class)) {
                dataField = field.getName();
            }
        }

        c.where(preds);
        E e;
        try {
            e = this.entityManager.createQuery(c).getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            log.warn("Changes not found for entity {} returning current entity", t);
            return t;
        }

        if (e == null) {
            log.warn("Changes not found for entity {} returning current entity", t);
            return t;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(e);
        data = (String) wrapper.getPropertyValue(dataField);
        if (null != data) {
            //Serialize object
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            T newBean = mapper.readValue(data, this.entityClazz);
            log.debug("Retrieved entity {} from data ({})", newBean, data);
            newBean = this.updateEdit(newBean, t);
            this.entityManager.remove(e);
            this.entityManager.merge(newBean);
            this.entityManager.flush();
            return newBean;
        } else {
            log.warn("Data field is empty returning current entity", t);
        }
        return t;
    }

    /**
     * Used to check has changes in the temp entity
     *
     * @param id     entity id
     * @return entity if changes exists
     * @throws javax.persistence.NoResultException if changes were not found
     */
    public List<E> getEntityChanges(Serializable id){
        CriteriaQuery<E> c = this.builder.createQuery(editedEnClazz);
        Root criteriaRoot = c.from(editedEnClazz);
        Predicate[] preds = new Predicate[2];

        for (Field field : editedEnClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EditEntity.class)) {
                preds[0] = this.builder.equal(criteriaRoot.get(field.getName()), this.entityClazz.getSimpleName());
            } else if (field.isAnnotationPresent(EditEntityId.class)) {
                if (field.getType().isAssignableFrom(String.class)) {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id.toString());
                } else {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id);
                }
            }
        }

        c.where(preds);
        return this.entityManager.createQuery(c).getResultList();
    }

    /**
     * Used to clear all entity changes with the specified entity id
     * @param id entity id
     */
    public void clearEntityChanges(Serializable id){
        CriteriaDelete<E> c = this.builder.createCriteriaDelete(editedEnClazz);
        Root criteriaRoot = c.from(editedEnClazz);
        Predicate[] preds = new Predicate[2];

        for (Field field : editedEnClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EditEntity.class)) {
                preds[0] = this.builder.equal(criteriaRoot.get(field.getName()), this.entityClazz.getSimpleName());
            } else if (field.isAnnotationPresent(EditEntityId.class)) {
                if (field.getType().isAssignableFrom(String.class)) {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id.toString());
                } else {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), id);
                }
            }
        }
        c.where(preds);
        this.entityManager.createQuery(c).executeUpdate();
    }

    /**
     * Update entity with changes from the new object
     *
     * @param oldBean previous record
     * @param newBean new record
     * @return updated bean
     * @throws IllegalAccessException occurs when the field annotated with
     *                                {@link ModifiableField} is not accessible
     */
    public T updateEdit(T newBean, T oldBean) throws IllegalAccessException {
        final Field[] allFields = newBean.getClass().getDeclaredFields();
        BeanWrapper wrapper = new BeanWrapperImpl(oldBean);
        for (Field field : allFields) {
            //Manage the fields that we need only
            if (field.isAnnotationPresent(ModifiableField.class)) {
                //Enable access of this field 
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Object oldValue = wrapper.getPropertyValue(field.getName());//field.get(oldBean);
                Object newValue = field.get(newBean);
                if (oldValue != newValue) {
                    if (field.isAnnotationPresent(OneToMany.class)) {//detach existing entities
                        this.deleteCollection((Collection) oldValue);
                    }
                    wrapper.setPropertyValue(field.getName(), newValue);
                }
            }
        }
        return oldBean;
    }

    /**
     * Used to delete a collection of entities
     *
     * @param entities {@link Collection} of entities
     */
    public void deleteCollection(Collection<?> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        //get ids
        List<Object> ids = new ArrayList<>();
        Class clazz = null;
        for (Object entity : entities) {
            if (clazz == null) {
                clazz = entity.getClass();
            }
            ids.add(this.getFieldValue(entity, Id.class, false));
        }
        //get id field
        String idField = "";
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idField = field.getName();
                break;
            }
        }
        CriteriaDelete<Object> criteria = this.builder.createCriteriaDelete(clazz);
        Root root = criteria.from(clazz);
        criteria.where(root.get(idField).in(ids));
        this.entityManager.createQuery(criteria).executeUpdate();
    }


    /**
     * Used to decline entity changes. It clears data from the EditEntity
     *
     * @param id entity id
     * @throws java.lang.IllegalArgumentException if modified fields cannot be accessed
     */
    public void declineChanges(Serializable id) throws IllegalArgumentException {
        E e = this.getEditedEntity(id);
        log.debug("Found edited record entity {}", e);
        if (e == null) {
            return;
        }
        this.entityManager.remove(e);
        this.entityManager.flush();
    }

    /**
     * Get edited entity with changes
     *
     * @param entityId edited entity id
     * @return {@link E} Edited entity
     */
    private E getEditedEntity(Serializable entityId) {
        CriteriaQuery<E> c = this.builder.createQuery(editedEnClazz);
        Root criteriaRoot = c.from(editedEnClazz);
        Predicate[] preds = new Predicate[2];

        for (Field field : editedEnClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(EditEntity.class)) {
                preds[0] = this.builder.equal(criteriaRoot.get(field.getName()), this.entityClazz.getSimpleName());
            } else if (field.isAnnotationPresent(EditEntityId.class)) {
                if (field.getType().isAssignableFrom(String.class)) {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), entityId.toString());
                } else {
                    preds[1] = this.builder.equal(criteriaRoot.get(field.getName()), entityId);
                }

            }
        }
        try {
            c.where(preds);
            return this.entityManager.createQuery(c).getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            log.warn("Changes not found for entity name {} and entityId {} returning current entity", entityId, this.entityClazz.getSimpleName());
            return null;
        }
    }

    /**
     * Used by jackson mapper to  filter relational entities
     */
    @JsonFilter("dynamicFilter")
    private class DynamicMixIn {
    }

}
