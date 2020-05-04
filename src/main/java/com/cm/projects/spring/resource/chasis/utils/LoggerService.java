/*
 * Copyright 2019 Cornelius M.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /**
     * Handles audit log with activity type update
     *
     * @param description description
     * @param entity entity name
     * @param entityId entity id
     * @param activityStatus activity status
     * @param notes event description
     */
    public void logUpdate(String description, String entity, Object entityId, Short activityStatus, String notes){
        this.log(description, entity, entityId, LoggerService.ACTIVITY_UPDATE, activityStatus, notes);
    }

    /**
     * Handle audit trail with activity update and activity status complete
     *
     * @param description description
     * @param entity entity name
     * @param entityId entity id
     * @param notes event description
     */
    public void logUpdateComplete(String description, String entity, Object entityId, String notes){
        this.log(description, entity, entityId, LoggerService.ACTIVITY_UPDATE, AppConstants.STATUS_ID_COMPLETED, notes);
    }

    /**
     * Handle audit trail with activity update and activity status failed
     *
     * @param description description
     * @param entity entity name
     * @param entityId entity id
     * @param notes event description
     */
    public void logUpdateFailed(String description, String entity, Object entityId, String notes){
        this.log(description, entity, entityId, LoggerService.ACTIVITY_UPDATE, AppConstants.STATUS_ID_FAILED, notes);
    }

    /**
     * Handle audit trail with activity create and activity status completed
     *
     * @param description description
     * @param entity entity name
     * @param entityId entity id
     */
    public void logCreateComplete(String description, String entity, Object entityId){
        this.log(description, entity, entityId, LoggerService.ACTIVITY_CREATE, AppConstants.STATUS_ID_COMPLETED, "");
    }

    /**
     * Handle audit trail with activity create and activity status failed
     *
     * @param description description
     * @param entity entity name
     * @param entityId entity id
     */
    public void logCreateFailed(String description, String entity, Object entityId){
        this.log(description, entity, entityId, LoggerService.ACTIVITY_CREATE, AppConstants.STATUS_ID_FAILED, "");
    }

    /**
     * Handles audit trail
     *
     * @param builder {@link Builder}
     */
    public void log(Builder builder) {
        this. log(builder.description, builder.entity, builder.entityId, builder.activity,
                builder.activityStatus, builder.notes);
    }

    /**
     * Event logs builder
     */
    public static class Builder{

        public String description;
        public String entity;
        public Object entityId;
        public String activity;
        public Short activityStatus;
        public String notes;

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setEntity(String entity) {
            this.entity = entity;
            return this;
        }

        public Builder setEntityId(Object entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder setActivity(String activity) {
            this.activity = activity;
            return this;
        }

        public Builder setActivityStatus(Short activityStatus) {
            this.activityStatus = activityStatus;
            return this;
        }

        public Builder setNotes(String notes) {
            this.notes = notes;
            return this;
        }

        public static Builder instance(){
            return new Builder();
        }
    }
}
