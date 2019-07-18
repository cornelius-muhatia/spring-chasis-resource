package com.cm.testprojects.entities;

import com.cm.projects.spring.resource.chasis.annotations.EditDataWrapper;
import com.cm.projects.spring.resource.chasis.annotations.EditEntity;
import com.cm.projects.spring.resource.chasis.annotations.EditEntityId;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class EdittedRecord {

    @Id
    @GeneratedValue
    private Long id;
    @EditDataWrapper
    private String data;
    @EditEntity
    private String entity;
    @EditEntityId
    private Long entityId;

    public EdittedRecord() {
    }

    public EdittedRecord(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
}
