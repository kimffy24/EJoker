package com.jiefzz.ejoker.infrastructure.z.common.context.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.jiefzz.ejoker.infrastructure.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.infrastructure.z.common.context.IAssemblyAnalyzer;
import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.context.Initialize;
import com.jiefzz.ejoker.infrastructure.z.common.context.impl.util.ClassNamesScanner;

public class AssemblyAnalyzerImpl implements IAssemblyAnalyzer {

	public AssemblyAnalyzerImpl(String packageName) {
		annotationScan(packageName);
	}

	@Override
	public Map<String, Map<String, Field>> getDependenceMapper() {
		return contextDependenceAnnotationMapping;
	}

	@Override
	public Map<String, Set<Method>> getInitializeMapper() {
		return contextInitializeAnnotationMapping;
	}

	@Override
	public Set<Class<?>> getEServiceMapper() {
		return contextEServiceAnnotationMapping;
	}


	private void annotationScan(String specificPackage) {
		List<Class<?>> clazzInSpecificPackage;
		try {
			clazzInSpecificPackage = ClassNamesScanner.scanClass(specificPackage);
		} catch (Exception e) {
			throw new ContextRuntimeException("Exception occur whild scanning package ["+specificPackage+"]!!!", e);
		}

		for (Class<?> clazz:clazzInSpecificPackage) {
			if(Exception.class.isAssignableFrom(clazz)) continue;
			if(Throwable.class.isAssignableFrom(clazz)) continue;
			if(clazz.isInterface()) continue;
			analyzeContextAnnotation(clazz);
		}
	}

	private void analyzeContextAnnotation(final Class<?> claxx) {
		Class<?> clazz = claxx;
		String className = clazz.getName();

		// collect the class which is set annotation @EService .
		if(clazz.isAnnotationPresent(EService.class))
			contextEServiceAnnotationMapping.add(claxx);

		// collect the method which annotate by @Initialize .
		Set<Method> annotationMethodName = new HashSet<Method>();
		// collect the properties which annotate by @Dependence
		Map<String, Field> annotationFieldName = new HashMap<String, Field>();
		for ( ; clazz != Object.class; clazz = clazz.getSuperclass() ) {
			Method[] methods = clazz.getDeclaredMethods();
			for ( Method method : methods ) {
				if ( annotationMethodName.contains(method) ) continue;
				if ( method.isAnnotationPresent(Initialize.class) )
					annotationMethodName.add(method);
			}
			Field[] fieldArray = clazz.getDeclaredFields();
			for ( Field field : fieldArray ) {
				if ( field.isAnnotationPresent(Dependence.class) || field.isAnnotationPresent(Resource.class) )
					annotationFieldName.putIfAbsent(field.getName(), field);
			}
		}
		if ( annotationFieldName.size()>0 ) contextDependenceAnnotationMapping.put(className, annotationFieldName);
		if ( annotationMethodName.size()>0 ) contextInitializeAnnotationMapping.put(className, annotationMethodName);

	}

	private final Map<String, Map<String, Field>> contextDependenceAnnotationMapping = new HashMap<String, Map<String, Field>>();
	private final Map<String, Set<Method>> contextInitializeAnnotationMapping = new HashMap<String, Set<Method>>();
	private final Set<Class<?>> contextEServiceAnnotationMapping = new HashSet<Class<?>>();
}
