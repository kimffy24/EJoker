package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.z.common.context.AbstractContext;
import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;

public class InstanceBuilderImpl implements IInstanceBuilder {

	private static final Map<String, Field> emptyDependenceInfo = new HashMap<String, Field>();
	
	private final AbstractContext context;
	private final Map<String, Field> dependenceInfo;
	private final Class<?> clazz;
	
	public InstanceBuilderImpl(AbstractContext context, Class<?> clazz, Map<String, Field> dependenceInfo){
		this.context = context;
		this.clazz = clazz;
		this.dependenceInfo = dependenceInfo!=null?dependenceInfo:emptyDependenceInfo;
	};
	
	
	@Override
	public Object doCreate() {
		try {
			Object instance = clazz.newInstance();
			adoptIntoContext(instance);
			doInjectDependence(instance);
			return instance;
		} catch (Exception e) {
			throw new ContextRuntimeException("Could not create instance of ["+clazz.getName()+"]", e);
		}
	}
	
	private Object doInjectDependence(Object instance){
		context.resolveWatingInject(clazz, instance);
		Set<String> ownWatingInjectFieldNames = dependenceInfo.keySet();
		for ( String fieldName : ownWatingInjectFieldNames ) {
			Field field = dependenceInfo.get(fieldName);
			if ( context.hasInstance(field.getType()) ) {
				try {
					field.setAccessible(true);
					field.set(instance, context.getInstance(field.getType()));
				} catch (Exception e) {
					throw new ContextRuntimeException("Could not create instance of ["+clazz.getName()+"]", e);
				}
			} else
				context.markWatingInject(field.getType().getName(), instance, field);
		}
		return instance;
	}
	
	private void adoptIntoContext(Object instance){
		
		context.adoptInstance(clazz, instance);
		Class<?>[] interfaces = clazz.getInterfaces();
		for ( Class<?> intf : interfaces )
			context.adoptInstance(intf, instance);
		
	}
}