package com.cm.testprojects.entities;


import com.cm.projects.spring.resource.chasis.annotations.ExportField;
import com.cm.projects.spring.resource.chasis.annotations.ModifiableField;
import com.cm.projects.spring.resource.chasis.annotations.NickName;
import com.cm.projects.spring.resource.chasis.annotations.Unique;
import com.cm.projects.spring.resource.chasis.utils.AppConstants;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

@Entity
@NickName(name = "User")
public class User {

    @Id
    @GeneratedValue
    private Long id;
    @NotNull
    @Size(min = 1, max = 100)
    @Unique(isCaseSensitive = false, fieldName =  "Name")
    @ModifiableField
    @ExportField
    private String name;
    @ManyToOne
    @NotNull
    @ModifiableField
    private Gender gender;
    @Temporal(TemporalType.TIMESTAMP)
    @ExportField
    private Date dateOfBirth;
    private String intrash = AppConstants.NO;
    @ManyToOne
    @Basic(optional = false)
    private Status status;

    private User(){

    }

    public User(Long id) {
        this.id = id;
    }

    public User(@NotNull @Size(min = 1, max = 100) String name, @NotNull Gender gender, Date dateOfBirth) {
        this.name = name;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getIntrash() {
        return intrash;
    }

    public void setIntrash(String intrash) {
        this.intrash = intrash;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gender=" + gender +
                ", dateOfBirth=" + dateOfBirth +
                ", intrash='" + intrash + '\'' +
                ", status=" + status +
                '}';
    }
}
