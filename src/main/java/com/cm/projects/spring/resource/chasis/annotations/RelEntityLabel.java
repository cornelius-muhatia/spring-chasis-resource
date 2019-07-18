package com.cm.projects.spring.resource.chasis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field on @{@link javax.persistence.ManyToOne} field best describes the field. For example <br />
 * For field <pre>@ManyToOne private Status statusEntity;</pre> <br />
 * On <b>Status</b> entity the label field can be:<br />
 * <pre>@RelEntityLabel private String status;</pre>
 */
@Target(value = {ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelEntityLabel {

    /**
     * For @ManyToOne  field
     * @return {@link String}
     */
    String fieldName() default "";
}
