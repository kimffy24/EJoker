package com.jiefzz.ejoker.annotation.persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jiefzz.ejoker.annotation.EJokerAnnotation;

/**
 * While doing persistent job, we do not find any properties from it's father.
 * @author JiefzzLon
 *
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@EJokerAnnotation
public @interface PersistentTop {

}
