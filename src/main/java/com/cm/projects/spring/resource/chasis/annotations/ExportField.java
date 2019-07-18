/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cm.projects.spring.resource.chasis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark a field to be considered when exporting fields
 * @author Cornelius M
 */
@Target(value = {ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportField {
    /**
     * Name of the field mainly used for column header
     * @return 
     */
    public String name() default "";
    /**
     * Used to mark an entity relational field. The field name will be used to 
     * mark the field to be used in the entity class
     * @return 
     */
    public ExportFieldEntity entityField() default @ExportFieldEntity;
//    long size() default -1;
//    int order() default 0;
}
