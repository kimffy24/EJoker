package com.jiefzz.ejoker.context.annotation.persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jiefzz.ejoker.context.annotation.EJokerAnnotation;

/**
 * While doing persistent job, we except to ignore some field!
 * @author JiefzzLon
 *
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@EJokerAnnotation
public @interface PersistentIgnore {

}
