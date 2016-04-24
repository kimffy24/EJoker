package com.jiefzz.ejoker.context.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.context.AbstractContext;
import com.jiefzz.ejoker.context.ContextRuntimeException;
import com.jiefzz.ejoker.context.IInstanceBuilder;

public class InstanceBuilderImpl implements IInstanceBuilder {

	private final AbstractContext context;
	private final Map<String, String> dependenceInfo;
	private final Set<String> initializeInfo;
	
	public InstanceBuilderImpl(AbstractContext context){
		this.context = context;
		this.dependenceInfo = new HashMap<String, String>();
		this.initializeInfo = new HashSet<String>();
	};

	public InstanceBuilderImpl(AbstractContext context, Map<String, String> dependenceInfo, Set<String> initializeInfo){
		this.context = context;
		this.dependenceInfo = dependenceInfo;
		this.initializeInfo = initializeInfo;
	};
	
	
	@Override
	public Object doCreate(Class<?> clazz) {
		try {
			return doInjectDependence(clazz.newInstance());
		} catch (Exception e) {
			throw new ContextRuntimeException("Could not create instance of ["+clazz.getName()+"]", e);
		}
	}
	
	private Object doInjectDependence(Object instance){
		return instance;
	}

}
