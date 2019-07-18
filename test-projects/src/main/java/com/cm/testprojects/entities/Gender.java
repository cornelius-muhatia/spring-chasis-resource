package com.cm.testprojects.entities;

import com.cm.projects.spring.resource.chasis.annotations.NickName;
import com.cm.projects.spring.resource.chasis.annotations.RelEntityLabel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@NickName(name = "Gender")
public class Gender {

    @Id
//    @GeneratedValue
    private Short id;
    @RelEntityLabel
    private String gender;
    @ManyToOne
    private Status status;
    private String intrash;

    public Gender(){

    }

    public Gender(Short id) {
        this.id = id;
    }

    public Gender(Short id, String gender, Status status) {
        this.id = id;
        this.gender = gender;
        this.status = status;
        this.intrash = "NO";
    }

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getGender() {
        return gender + ("+/-");
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getIntrash() {
        return intrash;
    }

    public void setIntrash(String intrash) {
        this.intrash = intrash;
    }

    @Override
    public String toString() {
        return "Gender{" +
                "id=" + id +
                ", gender='" + gender + '\'' +
                ", status=" + status +
                ", intrash='" + intrash + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Gender gender = (Gender) o;

        return id.equals(gender.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
