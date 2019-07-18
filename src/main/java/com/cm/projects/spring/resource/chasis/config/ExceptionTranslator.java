/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cm.projects.spring.resource.chasis.config;

import com.cm.projects.spring.resource.chasis.exceptions.GeneralBadRequest;
import com.cm.projects.spring.resource.chasis.exceptions.RunTimeBadRequest;
import com.cm.projects.spring.resource.chasis.utils.CustomEntry;
import com.cm.projects.spring.resource.chasis.utils.SharedMethods;
import com.cm.projects.spring.resource.chasis.wrappers.ResponseWrapper;
import org.apache.tomcat.util.http.fileupload.FileUploadBase.SizeLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;

/**
 * Controller advice to translate the server side exceptions to client-friendly
 * json structures.
 */
public class ExceptionTranslator {

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Internal server error
     * @param ex Exception
     * @return {@link ResponseEntity}
     */
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ResponseWrapper> handleAllExceptions(Exception ex) {
        log.error("Sorry internal server error occured", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(500);
        response.setMessage("Sorry internal server error occured");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * JPA id constraint translator
     * @param ex
     * @return
     */
    @ExceptionHandler(org.springframework.orm.jpa.JpaObjectRetrievalFailureException.class)
    public ResponseEntity<ResponseWrapper> entityRetriavalFailure(org.springframework.orm.jpa.JpaObjectRetrievalFailureException ex) {
        log.error("Constraint violation", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage("Failed to locate items with the specified id(s)");
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ResponseWrapper> fileuploadException(MultipartException ex) {
        log.error("Multipart error: ", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        if (ex.getCause().getCause() instanceof SizeLimitExceededException) {
            SizeLimitExceededException sizeEx = (SizeLimitExceededException) ex.getCause().getCause();
            response.setMessage("File size exceeded allowed limit (Permitted size: " + sizeEx.getPermittedSize() + " KB)");
        } else {
            response.setMessage("File upload validation failed. Ensure the file is within the specified size and type");
        }
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ResponseWrapper> processConstraintViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Constraint violation", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(424);
        response.setMessage("Dependency failed this may be caused by conflict with existing record or "
                + "missing fields in the request");
        return new ResponseEntity(response, HttpStatus.FAILED_DEPENDENCY);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseWrapper> processSpringValidationError(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage(), ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage("Bad request please check your input before trying again");
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.client.HttpClientErrorException.class)
    public ResponseEntity<ResponseWrapper> restTemplateErrors(org.springframework.web.client.HttpClientErrorException ex) {
        log.error(ex.getMessage(), ex);
        ResponseWrapper response = new ResponseWrapper();
        if (ex.getStatusCode().value() == 404) {
            response.setStatus(404);
            response.setMessage("Failed to locate third party resources");
            return new ResponseEntity(response, HttpStatus.NOT_FOUND);
        }
        response.setStatus(500);
        response.setMessage("Internal server error occured");
        return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseWrapper> processSpringValidationError(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        log.error(ex.getMessage(), ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setData(new CustomEntry("description", ex.getMessage()));
        response.setMessage("Missing request parameters");
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResponseWrapper> processExpectationError(HttpMediaTypeNotSupportedException ex) {
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(417);
        response.setMessage("Sorry unsupported media type");
        return new ResponseEntity(response, HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseWrapper> processMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(405);
        response.setMessage(ex.getMessage());
        return new ResponseEntity(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({NullPointerException.class, org.springframework.orm.jpa.JpaSystemException.class, 
        java.sql.SQLException.class, NumberFormatException.class})
    public ResponseEntity<ResponseWrapper> processNullPointerError(NullPointerException ex) {
        log.error("Null pointer exception occured", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(500);
        response.setMessage("Sorry internal server error occured please check your request before trying again");
        return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.hibernate.AssertionFailure.class)
    public ResponseEntity<ResponseWrapper> processNullId(org.hibernate.AssertionFailure ex) {
        log.error("Trying to reference id that doesn't exist", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage("Sorry bad request, the specified id not found");
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({GeneralBadRequest.class})
    public ResponseEntity<ResponseWrapper> processGeneralBadRequest(Exception ex) {
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage(ex.getMessage());
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RunTimeBadRequest.class)
    public ResponseEntity<ResponseWrapper> processGeneralBadRequest(RunTimeBadRequest ex) {
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage(ex.getMessage());
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({IllegalStateException.class, TransactionSystemException.class,
        java.sql.SQLRecoverableException.class, CannotCreateTransactionException.class,
        java.net.UnknownHostException.class, ClassCastException.class,
        org.springframework.jdbc.CannotGetJdbcConnectionException.class,
        org.springframework.dao.InvalidDataAccessResourceUsageException.class,
        org.hibernate.exception.GenericJDBCException.class, IllegalStateException.class,
        javax.persistence.PersistenceException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ResponseWrapper> processGeneralException(Exception ex) {
        log.error("General exception thrown", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(500);
        response.setMessage("Sorry internal server error");
        return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler({InvalidDataAccessApiUsageException.class})
    public ResponseEntity<ResponseWrapper> proccessInternalError(Exception ex) {
        log.error("Internal server error occured", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(500);
        response.setMessage("Sorry internal server error occured");
        return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper> validationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        log.error("Validation errors occurred", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage("Sorry validation errors occurred");
        response.setData(SharedMethods.getFieldMapErrors(ex.getBindingResult()));
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ResponseEntity<ResponseWrapper> validationException(org.springframework.validation.BindException ex) {
        log.error("Validation errors occurred", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(400);
        response.setMessage("Sorry validation errors occurred");
        response.setData(SharedMethods.getFieldMapErrors(ex.getBindingResult()));
        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ResponseWrapper> unImplementedException(UnsupportedOperationException ex) {
        log.warn("Unsupported Exception", ex);
        ResponseWrapper response = new ResponseWrapper();
        response.setStatus(HttpStatus.NOT_IMPLEMENTED.value());
        response.setMessage("Sorry the requested resource is not yet implemented");
        return new ResponseEntity(response, HttpStatus.NOT_IMPLEMENTED);
    }
}
