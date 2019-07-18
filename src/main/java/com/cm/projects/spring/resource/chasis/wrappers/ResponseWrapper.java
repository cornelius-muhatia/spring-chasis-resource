/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cm.projects.spring.resource.chasis.wrappers;

import java.io.Serializable;
import java.util.Date;
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
     */
    public static ResponseWrapper OK = new ResponseWrapper();
    /**
     * Used to instantiate {@link ResponseWrapper} with status 201
     */
    public static ResponseWrapper CREATED = new ResponseWrapper(201);
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
    private Long timestamp;
    
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
        this.timestamp = new Date().getTime();
    }

    public ResponseWrapper(HttpStatus status, String message, T data) {
        this.status = status.value();
        this.message = message;
        this.data = data;
    }

    /**
     * Return default response with status created (status: 201)
     * @param data
     * @return {@link ResponseWrapper}
     */
    public static ResponseWrapper created(Object data){
        ResponseWrapper r = new ResponseWrapper(201);
        r.setData(data);
        return r;
    }
    
    /**
     * Return default response with status failed dependency (status: 424)
     * @param data
     * @return {@link ResponseWrapper}
     */
    public static ResponseWrapper failedDependency(Object data){
        ResponseWrapper r = new ResponseWrapper(HttpStatus.FAILED_DEPENDENCY.value());
        r.setMessage("Sorry some dependent fields could not be found");
        r.setData(data);
        return r;
    }
    
    /**
     * Return default ok response with given data
     * @param data
     * @return 
     */
    public static ResponseWrapper ok(Object data){
        ResponseWrapper r = ResponseWrapper.OK;
        r.setData(data);
        return r;
    }
    
    /**
     * Returns default not found response with given message
     * @param message
     * @return 
     */
    public static ResponseWrapper  notFound(String message){
        ResponseWrapper r = new ResponseWrapper();
        r.setStatus(404);
        r.setMessage(message);
        return r;
    }

    /**
     * Returns default response with given status and message
     * @param status {@link HttpStatus}
     * @param message response message
     * @return {@link ResponseWrapper}
     */
    public static ResponseWrapper status(HttpStatus status, String message){
        ResponseWrapper r = new ResponseWrapper();
        r.setStatus(status);
        r.setMessage(message);
        return r;
    }
    

    /**
     * Get status status
     * @return 
     */
    public int getStatus() {
        return status;
    }

    /**
     * Set status code
     * @param status 
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Set status code
     * @param status
     */
    public void setStatus(HttpStatus status){
        this.status = status.value();
    }

    /**
     * get response message
     * @return 
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set response message
     * @param message 
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get response data
     * @return 
     */
    public T getData() {
        return data;
    }

    /**
     * Set response data
     * @param data 
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * Get response timestamp
     * @return 
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Set response timestamp
     * @param timestamp 
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ResponseWrapper{" + "code=" + status + ", message=" + message + ", data=" + data + ", timestamp=" + timestamp + '}';
    }
}