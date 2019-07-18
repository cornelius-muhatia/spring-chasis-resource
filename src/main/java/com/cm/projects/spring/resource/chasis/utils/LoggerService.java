/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cm.projects.spring.resource.chasis.utils;

/**
 * Used to handle logging requests and checking if maker is performing current requests
 * @author Cornelius M
 * @version 1.0.0
 */
public abstract class LoggerService {
    
    //activity type
    public static final String ACTIVITY_READ = "Read";
    public static final String ACTIVITY_CREATE = "Creation";
    public static final String ACTIVITY_UPDATE = "Update";
    public static final String ACTIVITY_DELETE = "Deletion";
    public static final String ACTIVITY_APPROVE = "Approve";
    public static final String ACTIVITY_DECLINE = "Decline";
    public static final String ACTIVITY_DEACTIVATE = "Deactivate";
    public static final String ACTIVITY_ACTIVATION = "Activation";
    public static final String ACTIVITY_SCHEDULE = "Schedule";
    public static final String ACTIVITY_CONFIRMATION = "Confirmation";
    public static final String ACTIVITY_FORWARD = "Forwarding";
    public static final String ACTIVITY_TALLYING = "Tallying";
    public static final String ACTIVITY_INITIATING = "Initiating";
    public static final String ACTIVITY_AUTHENTICATION = "Authentication";
    public static final String ACTIVITY_UNLOCK = "Unlock";
    public static final String ACTIVITY_LOCK = "Lock";
    public static final String ACTIVITY_AMEND = "Amend";    
    
    /**
     * Used to handle the logging request
     * @param description action description
     * @param entity entity/model being affected e.g. User
     * @param entityId entity ID i.e. value of the field annotated with {@link javax.persistence.Id}
     * @param activity activity being performed e.g. update, deletion, creation or deactivation
     * @param activityStatus activity status id e.g. {@link AppConstants#STATUS_ID_ACTIVE}
     * @param notes more information describing the action
     */
    public abstract void log(String description, String entity, Object entityId, String activity, Short activityStatus, String notes);

    /**
     * Used to check if the current user initiated the current action.
     * @param Entity entity being affected e.g. User
     * @param entityId entity ID i.e. value of the field annotated with {@link javax.persistence.Id}
     * @param statusId status id
     * @return {@link true} if the current user is the initiator
     */
    public abstract boolean isInitiator(String Entity, Object entityId, Short statusId);
    
    /**
     * Log authentication with activity {@link LoggerService#ACTIVITY_AUTHENTICATION}
     * @param description action description
     * @param entity entity/model being affected
     * @param entityId entity ID i.e. value of the field annotated with {@link javax.persistence.Id} 
     * @param activityStatus activity status id e.g. {@link AppConstants#STATUS_ID_ACTIVE}
     */
    public void logAuthentication(String description, String entity, Object entityId, Short activityStatus){
        this.log(description, entity, entityId, ACTIVITY_AUTHENTICATION, activityStatus, "");
    }
    
    /**
     * Used to handle audit log with activity type create
     *
     * @param description action description
     * @param entity entity/model being affected
     * @param entityId entityId entity ID i.e. value of the field annotated with {@link javax.persistence.Id} 
     * @param activityStatus activity status id e.g. {@link AppConstants#STATUS_ID_ACTIVE}
     */
    public void logCreate(String description, String entity, Object entityId, Short activityStatus){
        this.log(description, entity, entityId, ACTIVITY_CREATE, activityStatus, "");
    }
//
//    /**
//     *
//     * @param description
//     * @param entity
//     * @param entityId
//     * @param activityStatus
//     * @param notes
//     */
//    public void logCreate(String description, String entity, Object entityId, Short activityStatus, String notes){
//        
//    }
//
//    /**
//     * Used to save audit log with activity type write
//     *
//     * @param description
//     * @param entity
//     * @param entityId
//     * @param activityStatus
//     * @param userId
//     * @param notes
//     */
//    public void logCreate(String description, String entity, Object entityId, Short activityStatus, String userId, String notes){
//        
//    }
//
//    /**
//     * Used to save audit log with activity type write
//     *
//     * @param description
//     * @param entity
//     * @param entityId
//     * @param activityStatus
//     * @param userId
//     * @param ip
//     * @param agent
//     */
//    public void logCreate(String description, String entity, Object entityId, Short activityStatus, String userId,
//            String ip, String agent){
//        
//    }
//
//    /**
//     * Save log with activity type update and current authenticated user
//     *
//     * @param description
//     * @param Entity
//     * @param entityId
//     * @param activityStatus
//     */
//    public void logUpdate(String description, String Entity, Object entityId, Short activityStatus){
//        
//    }
//
//    /**
//     * Save log with activity type update
//     *
//     * @param description
//     * @param Entity
//     * @param entityId
//     * @param activityStatus
//     * @param userId
//     * @param notes
//     */
//    public void logUpdate(String description, String Entity, Object entityId, Short activityStatus, String userId, String notes){
//        
//    }
//
//    /**
//     * Save log with activity type update
//     *
//     * @param description
//     * @param Entity
//     * @param entityId
//     * @param activityStatus
//     * @param notes
//     */
//    public void logUpdate(String description, String Entity, Object entityId, Short activityStatus, String notes){
//        
//    }
}
