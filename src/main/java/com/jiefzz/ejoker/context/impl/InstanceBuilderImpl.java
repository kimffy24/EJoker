package com.jiefzz.ejoker.context.impl;

import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.context.AbstractContext;
import com.jiefzz.ejoker.context.ContextRuntimeException;
import com.jiefzz.ejoker.context.IInstanceBuilder;

public class InstanceBuilderImpl implements IInstanceBuilder {

	private final AbstractContext context;
	private final Map<String, String> dependenceInfo;
	private final Set<String> initializeInfo;
	private final Class<?> clazz;
	
	public InstanceBuilderImpl(AbstractContext context, Class<?> clazz, Map<String, String> dependenceInfo, Set<String> initializeInfo){
		this.context = context;
		this.clazz = clazz;
		this.dependenceInfo = dependenceInfo;
		this.initializeInfo = initializeInfo;
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
		return instance;
	}
	
	private void adoptIntoContext(Object instance){
		
		context.adoptInstance(clazz, instance);
		Class<?>[] interfaces = clazz.getInterfaces();
		for ( Class<?> intf : interfaces )
			context.adoptInstance(intf, instance);
		
	}
}
