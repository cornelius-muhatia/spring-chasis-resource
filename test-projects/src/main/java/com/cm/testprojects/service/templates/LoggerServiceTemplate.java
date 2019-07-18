package com.cm.testprojects.service.templates;

import com.cm.projects.spring.resource.chasis.utils.LoggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggerServiceTemplate extends LoggerService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void log(String description, String entity, Object entityId, String activity, Short activityStatus, String notes) {
        log.warn("\n==========> AUDIT LOG:<===========\n" +
                "Description: {}\n" +
                "Entity: {}\n" +
                "Entity ID: {}\n" +
                "Activity: {}\n" +
                "Activity Status: {}\n" +
                "Notes: {}\n" +
                "===========> END <===========", description, entity, entityId, activity, activityStatus, notes);
    }

    @Override
    public boolean isInitiator(String Entity, Object entityId, Short statusId) {
        return false;
    }
}
