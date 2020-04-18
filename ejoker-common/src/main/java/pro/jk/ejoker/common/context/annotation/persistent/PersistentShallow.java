package pro.jk.ejoker.common.context.annotation.persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pro.jk.ejoker.common.context.annotation.EJokerAnnotation;

/**
 * While doing persistent job, we just find properties is directory own itself!
 * @author JiefzzLon
 * @deprecated some mind is worng.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@EJokerAnnotation
public @interface PersistentShallow {

}
