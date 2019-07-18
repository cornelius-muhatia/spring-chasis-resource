package com.cm.projects.spring.resource.chasis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark entities with compound unique keys
 * @author  Cornelius M
 * @version 1.0.0
 */
@Target(value = {ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CompoundUnique {
    /**
     * List of fields making up the compound key
     * @return {@link UniquePair}
     */
    public UniquePair[] pairs();
}
