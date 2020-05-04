/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cm.projects.spring.resource.chasis.wrappers;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Used to wrap JSON response
 * @author Cornelius M
 * @version 0.0.1
 * @param <T> data type
 */
public class ResponseWrapper <T> implements Serializable{  
    /**
     * Used to instantiate {@link ResponseWrapper} with status 200
     * @deprecated use {@link ResponseWrapper#ok(Object)}
     */
    @Deprecated
    public static ResponseWrapper<Object> OK = new ResponseWrapper<>();
    /**
     * Used to instantiate {@link ResponseWrapper} with status 201
     * @deprecated  user {@link ResponseWrapper#created(Object)}
     */
    @Deprecated
    public static ResponseWrapper<Object> CREATED = new ResponseWrapper<>(201);
    /**
     * HTTP Status status
     */
    private int status;
    /**
     * Response message
     */
    private String message;
    /**
     * Response data
     */
    private T data = null;
    /**
     * Response timestamp
     */
    private Long timestamp = new Date().getTime();
    
    /**
     * Default constructor assigns timestamp with current timestamp, default success message and status status 200
     */
    public ResponseWrapper(){
        this.status = 200;
        this.message = "Request was successful";
        this.timestamp = new Date().getTime();
    }

    public ResponseWrapper(int status) {
        this.status = status;
        this.message = "Request was successful";
    }

    public ResponseWrapper(HttpStatus status, String message) {
        this.status = status.value();
        this.message = message;
    }

    public ResponseWrapper(HttpStatus status, String message, T data) {
        this.status = status.value();
        this.message = message;
        this.data = data;
    }

    /**
     * Return default response with status created (status: 201)
     * @param data response data
     * @param <E> data generic reference
     * @return {@link ResponseWrapper}
     */
    public static <E> ResponseWrapper<E> created(E data){
        ResponseWrapper<E> r = new ResponseWrapper<>(201);
        r.setData(data);
        return r;
    }
    
    /**
     * Return default response with status failed dependency (status: 424)
     * @param data response data
     * @param <E> response data generic reference
     * @return {@link ResponseWrapper}
     */
    public static <E> ResponseWrapper<E> failedDependency(E data){
        ResponseWrapper<E> r = new ResponseWrapper<>(HttpStatus.FAILED_DEPENDENCY.value());
        r.setMessage("Sorry some dependent fields could not be found");
        r.setData(data);
        return r;
    }
    
    /**
     * Return default ok response with given data
     * @param data response data
     * @param <E> generic reference
     * @return {@link ResponseWrapper}
     */
    public static <E> ResponseWrapper<E> ok(E data){
        ResponseWrapper<E> r = new ResponseWrapper<E>();
        r.setData(data);
        r.setStatus(200);
        return r;
    }
    
    /**
     * Returns default not found response with given message
     * @param message response data
     * @param <E> response data generic reference
     * @return {@link ResponseWrapper}
     */
    public static <E> ResponseWrapper<E>  notFound(String message){
        ResponseWrapper<E> r = new ResponseWrapper<>();
        r.setStatus(404);
        r.setMessage(message);
        return r;
    }

    /**
     * Returns default response with given status and message
     * @param status {@link HttpStatus}
     * @param message response message
     * @param <E>  response data generic reference
     * @return {@link ResponseWrapper}
     */
    public static <E> ResponseWrapper<E> status(HttpStatus status, String message){
        ResponseWrapper<E> r = new ResponseWrapper<>(status, message);
        r.setStatus(status);
        r.setMessage(message);
        return r;
    }
    

    /**
     * 
     * @return Http status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Set http status code
     * @param status Http status code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Set status code
     * @param status {@link HttpStatus}
     */
    public void setStatus(HttpStatus status){
        this.status = status.value();
    }

    /**
     * 
     * @return response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set response message
     * @param message response message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get response data
     * @return response data
     */
    public T getData() {
        return data;
    }

    /**
     * Set response data
     * @param data resposne data
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 
     * @return response timestamp
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Set response timestamp
     * @param timestamp response timestamp
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ResponseWrapper{" + "code=" + status + ", message=" + message + ", data=" + data + ", timestamp=" + timestamp + '}';
    }
}