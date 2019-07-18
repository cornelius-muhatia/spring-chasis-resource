package com.cm.testprojects.entities;

import com.cm.projects.spring.resource.chasis.annotations.NickName;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
@NickName(name = "Status")
public class Status implements com.cm.projects.spring.resource.chasis.wrappers.Status<Short> {

//    @GeneratedValue
    @Id
    private Short id;
    private String status;
    private String description;
    private Date creationDate = new Date();
    private String intrash;

    public Status(){

    }

    public Status(Short id) {
        this.id = id;
    }

    public Status(String status) {
        this.status = status;
        this.intrash = "NO";
    }

    public Status(Short id, String status) {
        this.id = id;
        this.status = status;
    }

    @Override
    public void setId(Short id) {
        this.id = id;
    }

    public Short getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getIntrash() {
        return intrash;
    }

    public void setIntrash(String intrash) {
        this.intrash = intrash;
    }

    @Override
    public String toString() {
        return "Status{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", creationDate=" + creationDate +
                ", intrash='" + intrash + '\'' +
                '}';
    }
}
