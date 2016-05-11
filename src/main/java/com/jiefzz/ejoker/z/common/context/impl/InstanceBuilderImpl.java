package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.EServiceInfoTuple;
import com.jiefzz.ejoker.z.common.context.IContextWorker;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;
import com.jiefzz.ejoker.z.common.utilities.Ensure;

public class InstanceBuilderImpl implements IInstanceBuilder {

	private final IContextWorker context;
	private final Class<?> clazz;
	private final Map<String, AssemblyAnalyzer> assemblyMapper;
	
	public InstanceBuilderImpl(IContextWorker context, Class<?> clazz, Map<String, AssemblyAnalyzer> assemblyMapper){
		Ensure.notNull(assemblyMapper, "assemblyMapper");
		this.context = context;
		this.clazz = clazz;
		this.assemblyMapper = assemblyMapper;
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
		// 给依赖自己的实例注入自己
		context.resolveDependMe(clazz, instance);
		// 然后把自己依赖的类型，从上下文中找出来注入或者标记等待注入。
		Set<Entry<String, Field>> entrySet = getDependenceMapper(clazz.getName()).entrySet();
		for ( Entry<String, Field> entry : entrySet ) {
			Field field = entry.getValue();
			if ( context.hasInstance(field.getType()) ) {
				try {
					field.setAccessible(true);
					field.set(instance, context.getInstance(field.getType()));
				} catch (Exception e) {
					throw new ContextRuntimeException("Could not create instance of ["+clazz.getName()+"]", e);
				}
			} else
				context.markWatingInject(context.resolve(field.getType()), instance, field);
		}
		return instance;
	}
	
	private void adoptIntoContext(Object instance){
		context.set(clazz, instance);
	}

	private Map<String, Field> getDependenceMapper(String classFullName){
		return getAssemblyInfo(classFullName).getDependenceMapper().get(classFullName);
	}
	
	private AssemblyAnalyzer getAssemblyInfo(String classFullName){
		AssemblyAnalyzer assemblyAnalyzer = null;
		Set<Entry<String, AssemblyAnalyzer>> entrySet = assemblyMapper.entrySet();
		for(Entry<String, AssemblyAnalyzer> entry : entrySet)
			if(classFullName.startsWith(entry.getKey()))
				return assemblyAnalyzer = entry.getValue();
		if(assemblyAnalyzer==null)
			throw new ContextRuntimeException("AssemblyInfo for ["+classFullName+"] is not found!!!Did you forget make it into to scan?");
		return assemblyAnalyzer;
	}
}
