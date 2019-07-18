package com.cm.projects.spring.resource.chasis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to group related fields
 *
 * @author  Cornelius M.
 * @version 1.0.0
 */
@Target(value = {ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniquePair {
    /**
     * Compound fields
     * @return {@link java.lang.reflect.Array} of fields
     */
    String[] fields();

    /**
     * Constraint user friendly name
     * @return constraint name
     */
    String constraintName();
}
