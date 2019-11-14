package pro.jiefzz.ejoker.common.context.annotation.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pro.jiefzz.ejoker.common.context.annotation.EJokerAnnotation;

/**
 * Tell the configureObject or contextObject to scan its handler method!!!
 * @author JiefzzLon
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@EJokerAnnotation
public @interface EInitialize {

	public int priority() default 50;
	
}
