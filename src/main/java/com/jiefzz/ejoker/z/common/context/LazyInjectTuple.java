package com.jiefzz.ejoker.z.common.context;

import java.lang.reflect.Field;

/**
 * On purpose, help to tell the context,
 * the field of instance waiting inject.
 * @author JiefzzLon
 *
 */
public class LazyInjectTuple {

	public Object instance;
	public Field field;
	
}
