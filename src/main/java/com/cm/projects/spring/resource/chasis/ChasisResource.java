/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cm.projects.spring.resource.chasis;

import com.cm.projects.spring.resource.chasis.annotations.*;
import com.cm.projects.spring.resource.chasis.exceptions.ExpectationFailed;
import com.cm.projects.spring.resource.chasis.exceptions.GeneralBadRequest;
import com.cm.projects.spring.resource.chasis.repository.SupportRepository;
import com.cm.projects.spring.resource.chasis.service.ChasisService;
import com.cm.projects.spring.resource.chasis.service.templates.ChasisServiceTemplate;
import com.cm.projects.spring.resource.chasis.utils.*;
import com.cm.projects.spring.resource.chasis.utils.export.CsvFlexView;
import com.cm.projects.spring.resource.chasis.wrappers.ActionWrapper;
import com.cm.projects.spring.resource.chasis.wrappers.ResponseWrapper;
import com.cm.projects.spring.resource.chasis.wrappers.Status;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Exposes the following resource actions;
 * <ul>
 * <li>Create new resource</li>
 * <li>Update existing resource</li>
 * <li>Delete existing resource</li>
 * <li>Approve checker actions</li>
 * <li>Decline checker actions</li>
 * <li>Fetch, filter, search and paginate resources</li>
 * </ul>
 *
 * @param <T> action entity
 * @param <E> id class
 * @param <R> Edited entity
 * @author Cornelius M
 * @version 0.0.1
 */
public class ChasisResource<T, E extends Serializable, R> {

    protected final List<Class> genericClasses;
    protected final String recordName;
    protected final ChasisService chasisService;
    /**
     * Used to handling logging requests
     */
    protected LoggerService loggerService;
    /**
     * Used to handle repository transactions
     */
    protected EntityManager entityManager;
    /**
     * Used to handle update requests
     */
    protected SupportRepository<T, E> supportRepo;
    /**
     * Events logs handler
     */
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Used to initialize:
     * <ul>
     *     <li>Record name from entity nickname. Refer to {@link NickName}</li>
     *     <li>Initialize generic classes {@link ChasisResource#genericClasses}</li>
     * </ul>
     * @param loggerService {@link LoggerService} bean
     * @param entityManager {@link EntityManager} for database queries
     * @param chasisService {@link ChasisService} for makerchecker actions
     */
    public ChasisResource(LoggerService loggerService, EntityManager entityManager, ChasisService chasisService) {
        this.loggerService = loggerService;
        this.entityManager = entityManager;
        this.genericClasses = SharedMethods.getGenericClasses(this.getClass());
        this.chasisService = chasisService;
        this.supportRepo = new SupportRepository(entityManager, this.genericClasses.get(0), this.genericClasses.get(2));
        NickName nickName = AnnotationUtils.findAnnotation(this.genericClasses.get(0), NickName.class);
        this.recordName = (nickName == null) ? "Record" : nickName.name();
    }

    /**
     * Initializes the default constructor with a new instance of {@link ChasisService}
     * @param loggerService {@link LoggerService} bean for audit trail logging
     * @param entityManager {@link EntityManager} for database queries
     */
    public ChasisResource(LoggerService loggerService, EntityManager entityManager) {
        this(loggerService, entityManager, new ChasisServiceTemplate(entityManager));
    }

    /**
     * Used to persist new entities to the database. The following validations
     * are carried out before an entity is persisted:
     * <h3>Validations</h3>
     * <ul>
     * <li>Javax Validation i.e @{@link javax.validation.constraints.NotNull}, @{@link javax.validation.constraints.Size}</li>
     * <li>Unique Fields (Fields annotated with @{@link Unique} annotation</li>
     * <li>Validates if @{@link ManyToOne} entity exists</li>
     * </ul>
     * <h5><i>Note</i></h5>
     * If an id field is present on the entity it will be reset to null.
     *
     * @param t New entity to be persisted
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>201 on success</li>
     * <li>409 on unique validation error</li>
     * <li>400 on validation error</li>
     * <li>404 for @{@link ManyToOne} entities that don't exist</li>
     * </ul>
     */
    @RequestMapping(method = RequestMethod.POST)
    @Transactional
    @Operation(summary = "Create New Record", description = "On success returns the id of created entity")
    public ResponseEntity<ResponseWrapper<E>> create(@Valid @RequestBody T t) {

        ResponseWrapper<E> response = new ResponseWrapper<>();
        BeanWrapper accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);

        try {
            //check if relational entities exists
            this.chasisService.validateRelEntities(t, accessor);
            //validate unique fields
            this.chasisService.validateUniqueFields(accessor);
        } catch (GeneralBadRequest ex) {
            loggerService.log(ex.getMessage(), t.getClass().getSimpleName(),
                    null, AppConstants.ACTIVITY_CREATE, AppConstants.STATUS_ID_FAILED, "");
            response.setStatus(ex.getHttpStatus());
            response.setMessage(ex.getMessage());
            response.setData(null);
            return ResponseEntity.status(ex.getHttpStatus()).body(response);
        }

        try {//set status field
            this.setStatus(accessor, AppConstants.STATUS_ID_NEW);
        } catch (org.springframework.beans.InvalidPropertyException | IllegalAccessException | InstantiationException
                | InvocationTargetException | NullPointerException ex) {
            log.debug("Field status on entity {} is not accessible skipping field", this.genericClasses.get(0));
        } catch (NoSuchMethodException e) {
            log.error("Expects Status Class to have a constructor that takes Short parameter (ID field)", e);
        }

        try {
            accessor.setPropertyValue("intrash", AppConstants.NO);
        } catch (org.springframework.beans.NotWritablePropertyException ex) {
            log.debug("Field action on entity {} is not accessible skipping field", this.genericClasses.get(0));
        }

        entityManager.persist(t);
        this.loggerService.log("Created " + recordName + " successfully",
                t.getClass().getSimpleName(), SharedMethods.getEntityIdValue(t),
                AppConstants.ACTIVITY_CREATE, AppConstants.STATUS_ID_COMPLETED, "");

        response.setData((E) this.chasisService.getEntityId(t));
        response.setStatus(201);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Used to fetch entity by id
     *
     * @param id Entity id
     * @return {@link ResponseEntity} with data field containing the entity
     * (data is null when entity could not be found) and status 200:
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Operation(summary = "Fetch single record using record id")
    public ResponseEntity<ResponseWrapper<T>> getEntity(@PathVariable("id") E id) {
        ResponseWrapper<T> response = new ResponseWrapper<>();
        response.setData(this.fetchEntity(id));
        return ResponseEntity.ok(response);
    }

    /**
     * Used to set status field
     *
     * @param accessor {@link PropertyAccessor}
     * @param statusId {@link Short} status id
     * @throws InvocationTargetException,InstantiationException if can't instantiate {@link Status} entity
     * @throws NoSuchMethodException setStatus() method doesn't exist
     * @throws IllegalAccessException if getStatus() and setStatus() is not accessible
     *
     */
    private void setStatus(PropertyAccessor accessor, Short statusId) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (accessor.getPropertyValue("status") instanceof Number || Objects.requireNonNull(accessor.getPropertyType("status")).isPrimitive()) {
            accessor.setPropertyValue("status", statusId);
        } else {
            accessor.setPropertyValue("status", this.chasisService.instantiateEntity(statusId, accessor.getPropertyType("status")));

        }
    }

    /**
     * Used to get status id
     *
     * @param accessor entity bean wrapper
     * @return (@ link Short } status id
     * @throws org.springframework.beans.NotReadablePropertyException if status field doesn't exist
     */
    private Short getStatusId(PropertyAccessor accessor) throws org.springframework.beans.InvalidPropertyException {
        try {
            Object status = accessor.getPropertyValue("status");
            if (status instanceof Number) {
                return Short.valueOf(status + "");
            } else {
                for (Field f : Objects.requireNonNull(status).getClass().getDeclaredFields()) {
                    if (f.isAnnotationPresent(Id.class)) {
                        PropertyAccessor sAccessor = PropertyAccessorFactory.forBeanPropertyAccess(status);
                        return Short.valueOf(sAccessor.getPropertyValue(f.getName()) + "");
                    }
                }
            }
        } catch (NullPointerException ex) {
            log.error("Failed to retrieve status field from entity ", ex);
        }
        return null;
    }

    /**
     * Used to update entities by saving new changes to the edited record
     * entity. For edited record to work The following annotation must be
     * present on the relevant fields to be used to store changes;
     * <ul>
     * <li> @{@link com.cm.projects.spring.resource.chasis.annotations.EditEntity} used to store the name of the entity being
     * updated preferably should be a string For example
     * <p>
     * <code>@{@link com.cm.projects.spring.resource.chasis.annotations.EditEntity} <br> private {@link String}
     * recordEntity;</code></p>
     * </li>
     * <li>@{@link com.cm.projects.spring.resource.chasis.annotations.EditDataWrapper} used to store changes in JSON format
     * preferably should be a {@link String} For example
     * <p>
     * <code>@{@link com.cm.projects.spring.resource.chasis.annotations.EditDataWrapper}<br>private {@link String} data;</code></p>
     * </li>
     * <li>@{@link com.cm.projects.spring.resource.chasis.annotations.EditEntityId} used to store entity id and you can use any
     * data type that extends {@link Serializable} For example
     * <p>
     * <code>@{@link com.cm.projects.spring.resource.chasis.annotations.EditEntityId}<br> private {@link Long} entityId; </code>
     * </p>
     * </li>
     * </ul>
     *
     * <h4>Note</h4>
     * For created and updated records that have not been approved the changes
     * are persisted to the entity directly without being stored in the edited
     * record entity. Also if entity doesn't have status field entity is persisted  directly
     *
     * @param t entity containing new changes
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * <li>400 on validation errors (Relies on javax validation)</li>
     * <li>409 on unique field validation errors</li>
     * <li>417 if the entity us pending approval actions or if changes were not
     * found on the current entity</li>
     * </ul>
     * @throws IllegalAccessException                                              if a field on the entity could not be
     *                                                                             accessed
     * @throws JsonProcessingException                                             if changes could not be converted to json
     *                                                                             string
     * @throws com.cm.projects.spring.resource.chasis.exceptions.ExpectationFailed When
     *                                                                             editEntity does not have fields; @{@link com.cm.projects.spring.resource.chasis.annotations.EditEntity},
     * {@link com.cm.projects.spring.resource.chasis.annotations.EditEntity}
     * and {@link com.cm.projects.spring.resource.chasis.annotations.EditEntityId}
     */
    @RequestMapping(method = RequestMethod.PUT)
    @Operation(summary = "Update record. If status field doesn't exist the entity is updated directly")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Record not found")
            ,
            @ApiResponse(responseCode = "417", description = "Record has unapproved actions or if record has not been modified")
    })
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> updateEntity(@RequestBody @Valid T t) throws IllegalAccessException, JsonProcessingException, ExpectationFailed, NoSuchMethodException, InstantiationException, InvocationTargetException {
        log.debug("Updating entity {}", t);
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();

        T dbT = this.fetchEntity((Serializable) SharedMethods.getEntityIdValue(t));
        if (dbT == null) {
            log.warn("Failed to find entity {} returning error response", dbT);
            loggerService.log("Updating " + recordName + " failed due to record doesn't exist", t.getClass().getSimpleName(),
                    null, AppConstants.ACTIVITY_UPDATE, AppConstants.STATUS_ID_FAILED, "");
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setMessage("Sorry failed to locate record with the specified id");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }


        try {
            log.info("Validating relational entities on entity {}", t);
            //check if relational entities exists
            BeanWrapper accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
            this.chasisService.validateRelEntities(t, accessor);
            log.info("Validating unique entities on entity {}", t);
            //validate unique fields
            this.chasisService.validateUniqueFields(accessor);
            log.debug("Done unique constraints validation for entity {}", t);
        } catch (GeneralBadRequest ex) {
            log.warn("Validation failed on entity {} returning error (Message: {}) response", t, ex.getMessage());
            loggerService.log(ex.getMessage(), t.getClass().getSimpleName(),
                    null, AppConstants.ACTIVITY_UPDATE, AppConstants.STATUS_ID_FAILED, "");
            response.setStatus(ex.getHttpStatus());
            response.setMessage(ex.getMessage());
            return ResponseEntity.status(ex.getHttpStatus()).body(response);
        }

        PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(dbT);
        Object status;
        Short statusId = null;
        try {
            status = accessor.getPropertyValue("status");
            if (status instanceof Number) {
                statusId = Short.valueOf(status + "");
            } else {
                for (Field f : Objects.requireNonNull(status).getClass().getDeclaredFields()) {
                    if (f.isAnnotationPresent(Id.class)) {
                        PropertyAccessor sAccessor = PropertyAccessorFactory.forBeanPropertyAccess(status);
                        statusId = Short.valueOf(sAccessor.getPropertyValue(f.getName()) + "");
                        break;
                    }
                }
            }
            if (statusId != null) {
                if ((!statusId.equals(AppConstants.STATUS_ID_NEW)) && (!statusId.equals(AppConstants.STATUS_ID_ACTIVE))) {
                    loggerService.log("Updating " + recordName + " failed due to record has unapproved actions",
                            t.getClass().getSimpleName(), SharedMethods.getEntityIdValue(t),
                            AppConstants.ACTIVITY_UPDATE, AppConstants.STATUS_ID_FAILED, "");
                    response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
                    response.setMessage("Sorry record has Unapproved actions");
                    return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
                } else if (statusId.equals(AppConstants.STATUS_ID_ACTIVE)) {
                    accessor.setPropertyValue("status", ((status instanceof Number) ? AppConstants.STATUS_ID_UPDATED :
                            this.chasisService.instantiateEntity(AppConstants.STATUS_ID_UPDATED, accessor.getPropertyType("status"))));
                }
            }
        } catch (org.springframework.beans.InvalidPropertyException ex) {
            log.debug("Field status on entity {} updating entity directly", this.genericClasses.get(0));
            this.supportRepo.updateEdit(t, dbT);
            return ResponseEntity.ok(response);
        }

        List<String> changes;
        if (AppConstants.STATUS_ID_NEW.equals(statusId)) {
            changes = supportRepo.fetchChanges(t, dbT);
            t = supportRepo.updateEdit(t, dbT);
        } else {
            log.debug("Found an active entity saving changes to edited record");
            this.entityManager.merge(dbT);
            changes = supportRepo.handleEditRequest(t, dbT, this.genericClasses.get(2));
            if (changes.isEmpty()) {
                response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
                response.setMessage("Sorry record has not been modified");
                return new ResponseEntity(response, HttpStatus.EXPECTATION_FAILED);
            }
        }

        loggerService.log("Updated " + recordName + " successfully. "
                        + String.join(",", changes),
                t.getClass().getSimpleName(), SharedMethods.getEntityIdValue(t),
                AppConstants.ACTIVITY_UPDATE, AppConstants.STATUS_ID_COMPLETED, "");
        response.setData(changes);
        return ResponseEntity.ok(response);

    }

    /**
     * Used to delete entities.
     * <h4>Note</h4>
     * If action and actionStatus fields don't exist the record is moved to
     * trash directly (flagging .
     * <b>If intrash field doesn't exist the record is deleted permanently</b>
     *
     * @param actions contains an array of entity id(s)
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>207 if not all records could be deleted</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * </ul>
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @Operation(summary = "Delete record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> deleteEntity(@RequestBody @Valid ActionWrapper<E> actions) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        List<String> errors = new ErrorList<>();
        for (E id : actions.getIds()) {
            T t = supportRepo.fetchEntity(id);
            if (t == null) {
                loggerService.log("Deleting " + recordName + " failed due to record doesn't exist", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_DELETE, AppConstants.STATUS_ID_FAILED, "");
                errors.add(recordName + " with id " + id + " doesn't exist");
                continue;
            }

            PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
            try {
                Short statusId = this.getStatusId(accessor);
                if (statusId == null || !statusId.equals(AppConstants.STATUS_ID_ACTIVE)) {
                    loggerService.log("Failed to delete " + recordName + ". Record has unapproved actions",
                            t.getClass().getSimpleName(), id, AppConstants.ACTIVITY_DELETE, AppConstants.STATUS_ID_FAILED, "");
                } else {
                    if (accessor.isReadableProperty("status")) {
                        this.setStatus(accessor, AppConstants.STATUS_ID_DELETED);
                    } else {
                        throw new org.springframework.beans.NotWritablePropertyException(t.getClass(), "status");
                    }
//                    this.entityManager.persist(t);
                    loggerService.log("Deleted " + recordName + " successfully", this.genericClasses.get(0).getSimpleName(),
                            id, AppConstants.ACTIVITY_DELETE, AppConstants.STATUS_ID_COMPLETED, "");
                }
            } catch (org.springframework.beans.NotWritablePropertyException | org.springframework.beans.NotReadablePropertyException e) {
                log.debug("Failed to find action and action status failed skipping "
                        + "updating action status proceeding to flag intrash field");
                try {
                    accessor.setPropertyValue("intrash", AppConstants.YES);
                } catch (org.springframework.beans.NotWritablePropertyException ex) {
                    log.warn("Failed to locate intrash field deleting the object permanently");
                    this.entityManager.remove(t);
                    this.entityManager.flush();
                }
            }
        }

        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }

    }

    /**
     * Used to approve actions (create, update, delete, deactivate). Also
     * ensures only the checker can approve an action
     *
     * @param actions containing entities id
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * <li>207 if some of the action could be approved successfuly. The data
     * fields contains more details on records that failed</li>
     * </ul>
     * @throws NoSuchMethodException getStatus() doesn't exist
     * @throws InstantiationException can't instantiate {@link Status}
     * @throws IllegalAccessException,InvocationTargetException getStatus() or setStatus() is not accessible
     */
    @Operation(summary = "Approve Record Actions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    @RequestMapping(value = "/approve-actions", method = RequestMethod.PUT)
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> approveActions(@RequestBody @Valid ActionWrapper<E> actions)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        List<String> errors = new ErrorList<>();

        for (E id : actions.getIds()) {
            T t = this.fetchEntity(id);
            try {
                if (t == null) {
                    loggerService.log("Failed to approve " + recordName + ". Failed to locate record with specified id",
                            this.genericClasses.get(0).getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_FAILED, actions.getNotes());
                    errors.add(recordName + " with id " + id + " doesn't exist");
                    continue;
                }

                BeanWrapper accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
                Short statusId = this.getStatusId(accessor);
                if (statusId == null) {
                    throw new ExpectationFailed("Sorry entity does not contain status field or is null");
                }

                if (loggerService.isInitiator(accessor.getWrappedClass().getSimpleName(), id, statusId) && !AppConstants.STATUS_ID_UNCONFIRMED.equals(this.getStatusId(accessor))) {
                    errors.add("Sorry failed to approve " + recordName + ". Maker can't approve their own record ");
                    loggerService.log("Failed to approve " + recordName + ". Maker can't approve their own record",
                            accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_FAILED, actions.getNotes());
                    continue;
                } else if (AppConstants.STATUS_ID_NEW.equals(statusId)) {//process new record
                    this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
                    this.processApproveNew(id, accessor, actions.getNotes());
                } else if (AppConstants.STATUS_ID_UPDATED.equals(statusId)) {//process updated record
                    this.processApproveChanges(id, t, actions.getNotes(), recordName);
                    this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
                } else if (AppConstants.STATUS_ID_DELETED.equals(statusId)) {
                    this.processApproveDeletion(id, accessor, actions.getNotes(), recordName);
                } else if (AppConstants.STATUS_ID_UNCONFIRMED.equals(statusId)) {
                    this.processConfirm(id, t, actions.getNotes(), recordName, accessor);
                } else if (statusId.equals(AppConstants.STATUS_ID_DEACTIVATE)) {
                    this.processDeactivation(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_ACTIVATE)) {
                    this.processActivation(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_LOCK)) {
                    this.processLock(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_UNLOCK)) {
                    this.processUnlock(id, accessor, actions.getNotes(), recordName);
                } else {
                    loggerService.log("Failed to approve " + recordName + ". Record doesn't have approve actions",
                            this.genericClasses.get(0).getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_FAILED, actions.getNotes());
                    errors.add(recordName + " with id " + id + " doesn't have approve actions");
                }
                log.debug("Persisting current entity {} ", t);
                this.entityManager.merge(t);
            } catch (ExpectationFailed ex) {
                errors.add(ex.getMessage());
            }
        }
        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }
    }

    /**
     * Used to process new records
     *
     * @param id       entity id
     * @param accessor entity {@link BeanWrapper}
     * @param notes    action notes
     */
    protected void processApproveNew(E id, BeanWrapper accessor, String notes) {
        loggerService.log("Done approving new  " + recordName + "", accessor.getWrappedClass().getSimpleName(),
                id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Approve edit changes
     *
     * @param id       ID of the entity being approved
     * @param entity   the entity to be merged with the changes
     * @param notes    approve notes for the audit trail
     * @param nickName entity meaningful name
     * @throws ExpectationFailed thrown when either fields annotated with
     * @{@link ModifiableField} cannot be accessed or when data stored in edited
     * entity cannot be mapped back to an entity
     */
    protected void processApproveChanges(E id, T entity, String notes, String nickName) throws ExpectationFailed {
        try {
            entity = supportRepo.mergeChanges(id, entity);
        } catch (IOException | IllegalArgumentException | IllegalAccessException ex) {
            log.error(AppConstants.AUDIT_LOG, "Failed to approve record changes", ex);
            throw new ExpectationFailed("Failed to approve record changes please contact the administrator for more help");
        }
        loggerService.log("Done approving " + nickName + " changes",
                entity.getClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Approve Deletion
     *
     * @param id       Entity id
     * @param notes    Deletion notes
     * @param nickName record user readable name
     */
    protected void processApproveDeletion(E id, BeanWrapper accessor, String notes, String nickName) {
        try {
            accessor.setPropertyValue("intrash", AppConstants.YES);
        } catch (NotWritablePropertyException ex) {
            log.debug("Entity {} doesn't have intrash field deleting record permanently");
            this.entityManager.detach(accessor.getWrappedInstance());
        }
        loggerService.log("Done approving " + nickName + " deletion.",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Confirm records and flag status to new
     *
     * @param id       Entity id
     * @param entity   Record
     * @param notes    action notes
     * @param nickName record readable name
     */
    protected void processConfirm(E id, T entity, String notes, String nickName, BeanWrapper accessor) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Done confirmation " + nickName + ".",
                entity.getClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Processing deactivation
     *
     * @param id       entity id
     * @param accessor bean wrapper
     * @param notes    action notes
     * @param nickName record readable name
     */
    protected void processDeactivation(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_DEACTIVATED);
        loggerService.log("Deactivated " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Activate a record
     *
     * @param id       entity id
     * @param accessor bean wrapper
     * @param notes    action notes
     * @param nickName entity readable name
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void processActivation(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Deactivated " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Lock record
     *
     * @param id       record id
     * @param accessor {@link BeanWrapper}
     * @param notes    checker notes
     * @param nickName record readable name
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void processLock(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_LOCKED);
        loggerService.log("Locked " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Unlock record
     *
     * @param id       entity id
     * @param accessor Entity BeanWrapper
     * @param notes    approval notes
     * @param nickName record readable name
     * @throws NoSuchMethodException     Updating status failed
     * @throws InstantiationException    Updating status failed
     * @throws IllegalAccessException    Updating status failed
     * @throws InvocationTargetException Updating status failed
     */
    protected void processUnlock(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Unlocked " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_COMPLETED, notes);
    }


    /**
     * Used to decline actions (create, update, delete, deactivate). Ensures
     * only the checker can decline an action
     *
     * @param actions
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * <li>207 if some of the action could be approved successfuly. The data
     * fields contains more details on records that failed</li>
     * </ul>
     * @throws com.cm.projects.spring.resource.chasis.exceptions.ExpectationFailed When
     *                                                                             entity doesn't have action or actionStatus fields
     */
    @RequestMapping(value = "/decline-actions", method = RequestMethod.PUT)
    @Transactional
    @Operation(summary = "Decline Record Actions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    public ResponseEntity<ResponseWrapper> declineActions(@RequestBody @Valid ActionWrapper<E> actions) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ResponseWrapper response = new ResponseWrapper();

        Class clazz = SharedMethods.getGenericClasses(this.getClass()).get(0);
        List<String> errors = new ErrorList<>();

        for (E id : actions.getIds()) {
            T t = supportRepo.fetchEntity(id);
            try {
                if (t == null) {
                    loggerService.log("Failed to decline " + recordName + ". Failed to locate record with specified id",
                            clazz.getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_FAILED, actions.getNotes());
                    errors.add(recordName + " with id " + id + " doesn't exist");
                    continue;
                }

                BeanWrapper accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
                Short statusId = this.getStatusId(accessor);
                if (statusId == null) {
                    throw new ExpectationFailed("Sorry entity does not contain status field or is null");
                }
                if (loggerService.isInitiator(clazz.getSimpleName(), id, statusId) && !statusId.equals(AppConstants.STATUS_ID_UNCONFIRMED)) {
                    errors.add("Sorry maker can't approve their own record ");
                    loggerService.log("Failed to approve " + recordName + ". Maker can't approve their own record",
                            SharedMethods.getEntityName(clazz), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_FAILED, actions.getNotes());
                    continue;
                } else if (statusId.equals(AppConstants.STATUS_ID_NEW)) {//process new record
                    this.processDeclineNew(id, actions.getNotes(), recordName, accessor);
                    try {
                        accessor.setPropertyValue("intrash", AppConstants.YES);
                    } catch (org.springframework.beans.NotWritablePropertyException ex) {
                        log.warn("Failed to locate intrash field deleting the object permanently");
                        this.entityManager.remove(t);
                        this.entityManager.flush();
                    }
                } else if (statusId.equals(AppConstants.STATUS_ID_UPDATED)) {//process updated record
                    this.processDeclineChanges(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_DELETED)) {
                    this.processDeclineDeletion(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_UNCONFIRMED)) {
                    this.processDeclineConfirmation(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_DEACTIVATE)) {
                    this.processDeclineDeactivation(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_ACTIVATE)) {
                    this.processDeclineActivation(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_LOCK)) {
                    this.processDeclineLock(id, accessor, actions.getNotes(), recordName);
                } else if (statusId.equals(AppConstants.STATUS_ID_UNLOCK)) {
                    this.processDeclineUnlocking(id, accessor, actions.getNotes(), recordName);
                } else {
                    loggerService.log("Failed to decline " + recordName + ". Record doesn't have approve actions",
                            clazz.getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_FAILED, actions.getNotes());
                    errors.add("Record doesn't have approve actions");
                }
                this.entityManager.merge(t);
            } catch (ExpectationFailed ex) {
                errors.add(ex.getMessage());
            }
        }
        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }
    }

    /**
     * Decline new records
     *
     * @param id       ID of the entity being declined
     * @param accessor entity bean wrapper
     * @param notes    decline notes for the audit trail
     * @param nickName entity meaningful name
     * @throws ExpectationFailed thrown when either fields annotated with
     * @{@link ModifiableField} cannot be accessed or when data stored in edited
     * entity cannot be mapped back to an entity
     */
    protected void processDeclineNew(E id, String notes, String nickName, BeanWrapper accessor) throws ExpectationFailed {
        try {
            accessor.setPropertyValue("intrash", AppConstants.YES);
        } catch (NotWritablePropertyException ex) {
            log.debug("Entity {} doesn't have intrash field deleting record permanently");
            this.entityManager.detach(accessor.getWrappedInstance());
        }
        loggerService.log("Declined new " + nickName + "",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Decline edit changes. Clears temporal changes stored in the entity record
     *
     * @param id       ID of the entity being declined
     * @param accessor entity bean wrapper
     * @param notes    approve notes for the audit trail
     * @param nickName entity meaningful name
     * @throws ExpectationFailed thrown when either fields annotated with
     * @{@link ModifiableField} cannot be accessed or when data stored in edited
     * entity cannot be mapped back to an entity
     */
    protected void processDeclineChanges(E id, BeanWrapper accessor, String notes, String nickName) throws ExpectationFailed, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            supportRepo.declineChanges(id);
        } catch (IllegalArgumentException ex) {
            log.error(AppConstants.AUDIT_LOG, "Failed to decline record changes", ex);
            throw new ExpectationFailed("Failed to decline record changes please contact the administrator for more help");
        }
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Done declining " + nickName + " changes",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_UPDATE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Decline Deletion
     *
     * @param id       ID of the entity being declined
     * @param notes    approve notes for the audit trail
     * @param nickName entity meaningful name
     *                 Annotation {@link ModifiableField} cannot be accessed or when data stored in edited
     *                 entity cannot be mapped back to an entity
     */
    protected void processDeclineDeletion(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Done declining " + nickName + " deletion.",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Process decline confirmation
     *
     * @param id       entity id
     * @param accessor Bean wrapper
     * @param notes    action notes
     * @param nickName record readable name
     */
    protected void processDeclineConfirmation(E id, BeanWrapper accessor, String notes, String nickName) {
        try {
            accessor.setPropertyValue("intrash", AppConstants.YES);
        } catch (NotWritablePropertyException ex) {
            log.debug("Entity {} doesn't have intrash field deleting record permanently");
            this.entityManager.detach(accessor.getWrappedInstance());
        }
        loggerService.log("Declined confirmation " + nickName + ".",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_APPROVE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Processing deactivation
     *
     * @param id       entity id
     * @param accessor bean wrapper
     * @param notes    action notes
     * @param nickName record readable name
     */
    protected void processDeclineDeactivation(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Declined deactivation of " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Decline record activation
     *
     * @param id       entity id
     * @param accessor bean wrapper
     * @param notes    action notes
     * @param nickName entity readable name
     * @throws NoSuchMethodException     failed to set status field
     * @throws InstantiationException    failed to set status field
     * @throws IllegalAccessException    failed to set status field
     * @throws InvocationTargetException failed to set status field
     */
    protected void processDeclineActivation(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_DEACTIVATED);
        loggerService.log("Declined deactivation " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Declined locking record
     *
     * @param id       record id
     * @param accessor {@link BeanWrapper}
     * @param notes    checker notes
     * @param nickName readable name
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    protected void processDeclineLock(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVE);
        loggerService.log("Declined locking " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Decline record activation
     *
     * @param id       entity id
     * @param accessor bean wrapper
     * @param notes    action notes
     * @param nickName entity readable name
     * @throws NoSuchMethodException     failed to set status field
     * @throws InstantiationException    failed to set status field
     * @throws IllegalAccessException    failed to set status field
     * @throws InvocationTargetException failed to set status field
     */
    protected void processDeclineUnlocking(E id, BeanWrapper accessor, String notes, String nickName) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this.setStatus(accessor, AppConstants.STATUS_ID_LOCKED);
        loggerService.log("Declined unlocking " + nickName + " successfully",
                accessor.getWrappedClass().getSimpleName(), id, AppConstants.ACTIVITY_UNLOCK, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Used to fetch entity updated changes
     *
     * @param id entity id to be effected
     * @return {@link ResponseEntity} with status 200 and a {@link List} of
     * changes (Returns an empty list if changes don't exist)
     * @throws java.lang.IllegalAccessException if a field on the entity could
     *                                          not be accessed
     * @throws java.io.IOException              if stored changes could not be read
     * @see SupportRepository#fetchChanges(Serializable, Object)
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}/changes")
    @Operation(summary = "Fetch Record Changes")
    public ResponseEntity<ResponseWrapper<List<String>>> fetchChanges(@PathVariable("id") E id) throws IllegalAccessException, IOException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        response.setData(supportRepo.fetchChanges(id, this.fetchEntity(id)));
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch entity excluding entities in trash
     *
     * @param id
     * @return
     */
    public T fetchEntity(Serializable id) {
        Class clazz = this.genericClasses.get(0);//Shar.getGenericClasses(this.getClass()).get(0);
        //get id field name
        String fieldId = null;
        boolean hasIntrash = false;
        for (Field field : clazz.getDeclaredFields()) {
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
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);
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
     * Used to retrieve all entity records.
     * <h4>Note</h4>
     * <ul>
     * <li>If needle parameter is present search will be done on fields
     * annotated with @{@link Searchable} (Search is case insensitive)</li>
     * <li>If fields annotated with @{@link Filter} exist the request will be
     * searched for parameters with similar name as the field name and if found
     * the results will be filtered using the filter. For example for field
     * <pre>@Filter private String name;</pre> expects the request name
     * parameter to be name. To filter by date range you need to provide to and
     * from request parameters with a valid String date (dd/MM/yyyy, dd/MM/yyyy HH:mm:ss.SSS, dd/MM/yyyy HH:mm:ss)</li>
     * </ul>
     *
     * @param pg      used to sort and limit the result
     * @param request HTTP Request used to get filter and search parameters.
     * @return
     * @throws ParseException if request param date cannot be casted to {@link Date}
     */
    @Operation(summary = "Fetch all Records", description = "")
    @Parameters({
            @Parameter(name = "size", in = ParameterIn.QUERY, required = false, description = "Page size default is 20"),
            @Parameter(name = "page", in = ParameterIn.QUERY, required = false, description = "Page number default is 0"),
            @Parameter(name = "sort", in = ParameterIn.QUERY, required = false, description = "Field name e.g status,asc/desc",
                    examples = @ExampleObject(value = "'property': 'status,asc/desc'"))
    })
    @GetMapping
    public ResponseEntity<ResponseWrapper<Page<T>>> findAll(@Parameter(hidden = true) Pageable pg,
                                                            @Parameter(hidden = true) HttpServletRequest request) throws ParseException {

        ResponseWrapper response = new ResponseWrapper();
        CriteriaBuilder criteriaBuilder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.genericClasses.get(0));
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(this.genericClasses.get(0));
        ArrayList<Predicate> searchPreds = new ArrayList<>();
        ArrayList<Predicate> filterPreds = new ArrayList<>();
        List<Order> ords = new ArrayList<>();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        Calendar cal = Calendar.getInstance();

        //retrieve filter and search params
        for (Field field : this.genericClasses.get(0).getDeclaredFields()) {
            if (field.isAnnotationPresent(Searchable.class) && request.getParameter("needle") != null) { //process search attributes
                searchPreds.add(criteriaBuilder.like(criteriaBuilder.upper(root.get(field.getName())),
                        "%" + request.getParameter("needle").toUpperCase() + "%"));
            }

            if (field.isAnnotationPresent(Filter.class)) {//process filter attributes
                if (field.getAnnotation(Filter.class).isDateRange() && request.getParameter("to") != null
                        && request.getParameter("from") != null) {//filter date range

                    Date from = this.chasisService.tryParse(request.getParameter("from"));
                    if (from == null) {
                        throw new ParseException("Failed to parse " + request.getParameter("from") + " to date", 0);
                    }
                    cal.setTime(from);
                    cal.add(Calendar.DAY_OF_WEEK, -1);
                    from = cal.getTime();

                    Date to = this.chasisService.tryParse(request.getParameter("to"));
                    if (to == null) {
                        throw new ParseException("Failed to parse " + request.getParameter("to") + " to date", 0);
                    }
                    cal.setTime(to);
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                    to = cal.getTime();
                    Predicate datePred = criteriaBuilder.between(root.get(field.getName()).as(Date.class), from, to);
                    filterPreds.add(datePred);
                } else if (request.getParameter(field.getName()) != null && !request.getParameter(field.getName()).isEmpty()) {
                    if (field.isAnnotationPresent(ManyToOne.class)) {
//                        BeanWrapper wrapper = new BeanWrapperImpl(field.getType());
                        String fieldName = "";
                        for (Field f : field.getType().getDeclaredFields()) {
                            if (f.isAnnotationPresent(Id.class)) {
                                fieldName = f.getName();
                                break;
                            }
                        }
                        if(request.getParameterValues(field.getName()).length > 1) {
                            ArrayList<Object> relFilters = new ArrayList<>();
                            for(String paramFilter : request.getParameterValues(field.getName())){
                                BeanWrapper wrapper = new BeanWrapperImpl(field.getType());
                                wrapper.setPropertyValue(fieldName, paramFilter);
                                relFilters.add(wrapper.getWrappedInstance());
                            }
                            log.debug("Found array filter ({}) using in/contains predicate", relFilters);
                            filterPreds.add(root.get(field.getName()).in(relFilters));
                        } else{
                            BeanWrapper wrapper = new BeanWrapperImpl(field.getType());
                            wrapper.setPropertyValue(fieldName, request.getParameter(field.getName()));
                            filterPreds.add(criteriaBuilder.equal(root.get(field.getName()), wrapper.getWrappedInstance()));

                        }
                    } else {
                        if(request.getParameterValues(field.getName()).length > 1){
                            filterPreds.add(root.get(field.getName()).in(request.getParameterValues(field.getName())));
                        } else {
                            filterPreds.add(criteriaBuilder.like(root.get(field.getName()).as(String.class),
                                    request.getParameter(field.getName())));
                        }
                    }
                } else {
                    log.debug("Failed to find parameter {} skipping filtering for field {} ", field.getName(), field.getName());
                }
            }
            //check if order parameter is available for the current field
            Sort.Order ord = pg.getSort().getOrderFor(field.getName());
            if (ord != null) {
                log.debug("Found ordering paramater ({}) for field {} preparing ordering query", ord, field.getName());
                if (ord.isAscending()) {
                    ords.add(criteriaBuilder.asc(root.get(field.getName())));
                } else {
                    ords.add(criteriaBuilder.desc(root.get(field.getName())));
                }
            }
            //check if is intrash
            if (field.getName().equalsIgnoreCase("intrash")) {
                filterPreds.add(criteriaBuilder.equal(root.get(field.getName()), AppConstants.NO));
            }
        }
        if (!filterPreds.isEmpty() && !searchPreds.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.and(filterPreds.toArray(new Predicate[filterPreds.size()])),
                    criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
            countQuery.where(criteriaBuilder.and(filterPreds.toArray(new Predicate[filterPreds.size()])),
                    criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
        } else if (!filterPreds.isEmpty()) {
            criteriaQuery.where(filterPreds.toArray(new Predicate[filterPreds.size()]));
            countQuery.where(filterPreds.toArray(new Predicate[filterPreds.size()]));
        } else if (!searchPreds.isEmpty()) {
            criteriaQuery.where(criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
            countQuery.where(criteriaBuilder.or(searchPreds.toArray(new Predicate[searchPreds.size()])));
        } else {
            criteriaQuery.where();
            countQuery.where();
        }

        criteriaQuery.orderBy(ords);
        List<T> content = this.entityManager
                .createQuery(criteriaQuery)
                .setFirstResult((pg.getPageNumber() * pg.getPageSize()))
                .setMaxResults(pg.getPageSize())
                .getResultList();

        countQuery.select(criteriaBuilder.count(countQuery.from(this.genericClasses.get(0))));
        Long total = this.entityManager.createQuery(countQuery).getSingleResult();

        Page<T> page = new PageImpl<>(content, pg, total);

        response.setData(page);
        return ResponseEntity.ok(response);
    }

    /**
     * Used to deactivate entities.
     *
     * @param actions contains an array of entity id(s)
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>207 if not all records could be deactivated</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * </ul>
     */
    @PutMapping("/deactivate")
    @Operation(summary = "Deactivate record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> deactivateRecord(@RequestBody @Valid ActionWrapper<E> actions) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        List<String> errors = new ErrorList<>();
        for (E id : actions.getIds()) {
            T t = supportRepo.fetchEntity(id);
            if (t == null) {
                loggerService.log("Deactivating " + recordName + " failed due to record doesn't exist", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_FAILED, "");
                errors.add(recordName + " with id " + id + " doesn't exist");
                continue;
            }

            PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
            try {
                Short statusId = this.getStatusId(accessor);
                if (statusId == null || !statusId.equals(AppConstants.STATUS_ID_ACTIVE)) {
                    loggerService.log("Failed to deactivate " + recordName + ". Record has unapproved actions",
                            t.getClass().getSimpleName(), id, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_FAILED, "");
                    errors.add(recordName + " with id " + id + " has unapproved actions");
                } else {
                    this.setStatus(accessor, AppConstants.STATUS_ID_DEACTIVATE);
                    loggerService.log("Deactivate " + recordName + " successfully", this.genericClasses.get(0).getSimpleName(),
                            id, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_COMPLETED, "");
                }
            } catch (org.springframework.beans.NotWritablePropertyException | org.springframework.beans.NotReadablePropertyException e) {
                loggerService.log("Deactivating " + recordName + " failed due to record doesn't have status field", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_DEACTIVATE, AppConstants.STATUS_ID_FAILED, "");
                errors.add("Failed to find status on " + recordName + " (Id: " + id + ")");
            }
        }

        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }

    }

    /**
     * Used to activate entities.
     *
     * @param actions contains an array of entity id(s)
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>207 if not all records could be deactivated</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * </ul>
     */
    @PutMapping("/activate")
    @Operation(summary = "Activate records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> activateRecord(@RequestBody @Valid ActionWrapper<E> actions) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        List<String> errors = new ErrorList<>();
        for (E id : actions.getIds()) {
            T t = supportRepo.fetchEntity(id);
            if (t == null) {
                loggerService.log("Activating " + recordName + " failed due to record doesn't exist", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_ACTIVATION, AppConstants.STATUS_ID_FAILED, "");
                errors.add(recordName + " with id " + id + " doesn't exist");
                continue;
            }

            PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
            try {
                Short statusId = this.getStatusId(accessor);
                if (statusId == null || !statusId.equals(AppConstants.STATUS_ID_DEACTIVATED)) {
                    loggerService.log("Failed to activate " + recordName + ". Record is not deactivated",
                            t.getClass().getSimpleName(), id, AppConstants.ACTIVITY_ACTIVATION, AppConstants.STATUS_ID_FAILED, "");
                    errors.add(recordName + " with id " + id + " has not been deactivated ");
                } else {
                    this.setStatus(accessor, AppConstants.STATUS_ID_ACTIVATE);
                    loggerService.log("Activated " + recordName + " successfully", this.genericClasses.get(0).getSimpleName(),
                            id, AppConstants.ACTIVITY_ACTIVATION, AppConstants.STATUS_ID_COMPLETED, "");
                }
            } catch (org.springframework.beans.NotWritablePropertyException | org.springframework.beans.NotReadablePropertyException e) {
                loggerService.log("Activating " + recordName + " failed due to record doesn't have status field", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_ACTIVATION, AppConstants.STATUS_ID_FAILED, "");
                errors.add("Failed to find status field on " + recordName + " (Id: " + id + ")");
            }
        }

        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }

    }

    /**
     * Used to lock entities.
     *
     * @param actions contains an array of entity id(s)
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>207 if not all records could be locked</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * </ul>
     */
    @PutMapping("/lock")
    @Operation(summary = "Lock record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> lockRecord(@RequestBody @Valid ActionWrapper<E> actions) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        List<String> errors = new ErrorList<>();
        for (E id : actions.getIds()) {
            T t = supportRepo.fetchEntity(id);
            if (t == null) {
                loggerService.log("Locking " + recordName + " failed due to record doesn't exist", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_FAILED, "");
                errors.add(recordName + " with id " + id + " doesn't exist");
                continue;
            }

            PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
            try {
                Short statusId = this.getStatusId(accessor);
                if (statusId == null || !statusId.equals(AppConstants.STATUS_ID_ACTIVE)) {
                    loggerService.log("Failed to lock " + recordName + ". Record has unapproved actions",
                            t.getClass().getSimpleName(), id, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_FAILED, "");
                    errors.add(recordName + " with id " + id + " has unapproved actions");
                } else {
                    this.setStatus(accessor, AppConstants.STATUS_ID_LOCK);
                    loggerService.log("Locked " + recordName + " successfully", this.genericClasses.get(0).getSimpleName(),
                            id, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_COMPLETED, "");
                }
            } catch (org.springframework.beans.NotWritablePropertyException | org.springframework.beans.NotReadablePropertyException e) {
                loggerService.log("Locking " + recordName + " failed due to record doesn't have status field", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_LOCK, AppConstants.STATUS_ID_FAILED, "");
                errors.add("Failed to find status on " + recordName + " (Id: " + id + ")");
            }
        }

        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }

    }

    /**
     * Used to unlock entities.
     *
     * @param actions contains an array of entity id(s)
     * @return {@link ResponseEntity} with statuses:
     * <ul>
     * <li>200 on success</li>
     * <li>207 if not all records could be unlocked</li>
     * <li>404 if the entity doesn't exist in the database</li>
     * </ul>
     */
    @PutMapping("/unlock")
    @Operation(summary = "Unlock record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "207", description = "Some records could not be processed successfully")
    })
    @Transactional
    public ResponseEntity<ResponseWrapper<List<String>>> unLockRecord(@RequestBody @Valid ActionWrapper<E> actions) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        ResponseWrapper<List<String>> response = new ResponseWrapper<>();
        List<String> errors = new ErrorList<>();
        for (E id : actions.getIds()) {
            T t = supportRepo.fetchEntity(id);
            if (t == null) {
                loggerService.log("Unlocking " + recordName + " failed due to record doesn't exist", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_UNLOCK, AppConstants.STATUS_ID_FAILED, "");
                errors.add(recordName + " with id " + id + " doesn't exist");
                continue;
            }

            PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(t);
            try {
                Short statusId = this.getStatusId(accessor);
                if (statusId == null || !statusId.equals(AppConstants.STATUS_ID_LOCKED)) {
                    loggerService.log("Failed to unlocked " + recordName + ". Current record is not locked",
                            t.getClass().getSimpleName(), id, AppConstants.ACTIVITY_UNLOCK, AppConstants.STATUS_ID_FAILED, "");
                    errors.add(recordName + " with id " + id + " is not locked");
                } else {
                    this.setStatus(accessor, AppConstants.STATUS_ID_UNLOCK);
                    loggerService.log("Unlocked " + recordName + " successfully", this.genericClasses.get(0).getSimpleName(),
                            id, AppConstants.ACTIVITY_UNLOCK, AppConstants.STATUS_ID_COMPLETED, "");
                }
            } catch (org.springframework.beans.NotWritablePropertyException | org.springframework.beans.NotReadablePropertyException e) {
                loggerService.log("Unlocking " + recordName + " failed due to record doesn't have status field", this.genericClasses.get(0).getSimpleName(),
                        null, AppConstants.ACTIVITY_UNLOCK, AppConstants.STATUS_ID_FAILED, "");
                errors.add("Failed to find status on " + recordName + " (Id: " + id + ")");
            }
        }

        if (errors.isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            response.setStatus(HttpStatus.MULTI_STATUS.value());
            response.setData(errors);
            response.setMessage(AppConstants.CHECKER_GENERAL_ERROR);
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
        }

    }

    @Operation(summary = "Export Record To CSV")
    @GetMapping("/export.csv")
    public ModelAndView exportCsv(Pageable pg, HttpServletRequest request) throws ParseException {
        return new ModelAndView(new CsvFlexView<T>(this.genericClasses.get(0), this.chasisService.findAll(pg, this.genericClasses.get(0), request, entityManager), this.recordName));
    }

}
