package com.jiefzz.ejoker.context.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.context.AbstractContext;
import com.jiefzz.ejoker.context.ContextRuntimeException;
import com.jiefzz.ejoker.context.IInstanceBuilder;

public class InstanceBuilderImpl implements IInstanceBuilder {

	private static final Map<String, String> emptyDependenceInfo = new HashMap<String, String>();
	private static final Set<String> emptyInitializeInfo = new HashSet<String>();
	
	private final AbstractContext context;
	private final Map<String, String> dependenceInfo;
	private final Set<String> initializeInfo;
	private final Class<?> clazz;
	
	public InstanceBuilderImpl(AbstractContext context, Class<?> clazz, Map<String, String> dependenceInfo, Set<String> initializeInfo){
		this.context = context;
		this.clazz = clazz;
		this.dependenceInfo = dependenceInfo!=null?dependenceInfo:emptyDependenceInfo;
		this.initializeInfo = initializeInfo!=null?initializeInfo:emptyInitializeInfo;
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
			String typeName = dependenceInfo.get(fieldName);
			if ( context.hasInstance(typeName) ) {
				try {
					Field field = clazz.getDeclaredField(fieldName);
					field.setAccessible(true);
					field.set(instance, context.getInstance(typeName));
				} catch (Exception e) {
					throw new ContextRuntimeException("Could not create instance of ["+clazz.getName()+"]", e);
				}
			} else
				context.markWatingInject(typeName, instance, fieldName);
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
