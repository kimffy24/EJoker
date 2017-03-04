package com.jiefzz.ejoker.z.common.context.annotation.persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jiefzz.ejoker.z.common.context.annotation.EJokerAnnotation;

/**
 * While doing persistent job, we except to ignore some field!
 * @author JiefzzLon
 *
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@EJokerAnnotation
public @interface PersistentIgnore {

}
