package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.context.annotation.context.Initialize;
import com.jiefzz.ejoker.z.common.utilities.ClassNamesScanner;

/**
 * 存储注解信息的对象
 * @author JiefzzLon
 *
 */
public class AssemblyAnalyzer {

	private final Map<Class<?>, Map<String, Field>> contextDependenceAnnotationMapping = new HashMap<Class<?>, Map<String, Field>>();
	private final Map<Class<?>, Set<Method>> contextInitializeAnnotationMapping = new HashMap<Class<?>, Set<Method>>();
	private final List<Class<?>> contextEServiceAnnotationMapping = new ArrayList<Class<?>>();
	
	public AssemblyAnalyzer(String packageName) {
		annotationScan(packageName);
	}

	public Map<Class<?>, Map<String, Field>> getDependenceMapper() {
		return contextDependenceAnnotationMapping;
	}

	public Map<Class<?>, Set<Method>> getInitializeMapper() {
		return contextInitializeAnnotationMapping;
	}

	public List<Class<?>> getEServiceMapper() {
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
			// skip Throwable \ Abstract \ Interface class
			if(Throwable.class.isAssignableFrom(clazz)) continue;
			if(Modifier.isAbstract(clazz.getModifiers())) continue;
			if(clazz.isInterface()) continue;
			analyzeContextAnnotation(clazz);
		}
	}

	private void analyzeContextAnnotation(final Class<?> claxx) {
		Class<?> clazz = claxx;

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
		if ( annotationFieldName.size()>0 ) contextDependenceAnnotationMapping.put(claxx, annotationFieldName);
		if ( annotationMethodName.size()>0 ) contextInitializeAnnotationMapping.put(claxx, annotationMethodName);

	}
}
