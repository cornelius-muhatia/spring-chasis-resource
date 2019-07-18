/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cm.projects.spring.resource.chasis.utils;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Used to define constants to be used within the library
 * @author Cornelius M
 * @version 0.0.1
 */
public class AppConstants {
    public static final Marker AUDIT_LOG = MarkerFactory.getMarker("AUDIT_LOG");
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
    //Record Status
    public static final String STATUS_LOCKED = "Locked";
    public static final String STATUS_DISABLED = "Disabled";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_UNAPPROVED = "Unapproved";
    public static final String STATUS_FAILED = "Failed";
    public static final String STATUS_NEW = "New";
    public static final String STATUS_DECLINED = "Rejected";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_EXPIRED = "Expired";
    public static final String STATUS_DEACTIVATED = "Deactivated";
    public static final String STATUS_VERIFIED = "Verified";
    public static final String STATUS_CONFIRMED = "Confirmed";
    public static final String STATUS_CANCELLED = "Cancelled";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_UNCONFIRMED = "Unconfirmed";

    /**
     * Status new id
     */
    public static final Short STATUS_ID_NEW = 1;
    /**
     * Status active id
     */
    public static final Short STATUS_ID_ACTIVE = 2;
    /**
     * Status inactive id
     */
    public static final Short STATUS_ID_DEACTIVATED = 3;
    /**
     * Status completed id
     */
    public static final Short STATUS_ID_COMPLETED = 4;
    /**
     * Status deactivate id
     */
    public static final Short STATUS_ID_DEACTIVATE = 5;
    /**
     * Status declined id
     */
    public static final Short STATUS_ID_FAILED = 6;
    /**
     * Status updated id
     */
    public static final Short STATUS_ID_UPDATED = 7;
    /**
     * Status deleted id
     */
    public static final Short STATUS_ID_DELETED = 8;
    /**
     * Status unconfirmed
     */
    public static final Short STATUS_ID_UNCONFIRMED = 9;
    /**
     * Status activate id
     */
    public static final Short STATUS_ID_ACTIVATE = 10;
    /**
     * Status expired
     */
    public static final Short STATUS_ID_EXPIRED =  11;
    /**
     * Status lock
     */
    public static final Short STATUS_ID_LOCK = 12;
    /**
     * Status locked
     */
    public static final Short STATUS_ID_LOCKED = 13;
    /**
     * Status unlocked
     */
    public static final Short STATUS_ID_UNLOCK = 14;

    public static final String NO = "NO";
    public static final String YES = "YES";
    
    public static final String CHECKER_GENERAL_ERROR = "Some Actions could not be processed successfully check audit logs for more details";

}
