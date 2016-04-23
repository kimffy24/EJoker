package com.jiefzz.ejoker.context.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.annotation.context.Dependence;
import com.jiefzz.ejoker.annotation.context.Initialize;
import com.jiefzz.ejoker.context.ContextRuntimeException;
import com.jiefzz.ejoker.context.IAssemblyAnalyzer;
import com.jiefzz.ejoker.context.impl.util.ClassesScanner;

public class AssemblyAnalyzerImpl implements IAssemblyAnalyzer {

	public AssemblyAnalyzerImpl(String packageName) {
		annotationScan(packageName);
	}
	
	@Override
	public Map<String, Set<String>> getDependenceMapper() {
		return contextDependenceAnnotationMapping;
	}

	@Override
	public Map<String, Set<String>> getInitializeMapper() {
		return contextInitializeAnnotationMapping;
	}


	private void annotationScan(String specificPackage) {
		List<Class<?>> clazzInSpecificPackage;
		try {
			clazzInSpecificPackage = ClassesScanner.scanClass(specificPackage);
		} catch (Exception e) {
			throw new ContextRuntimeException("Exception occur whild scanning package ["+specificPackage+"]!!!", e);
		}
		
		for (Class<?> clazz:clazzInSpecificPackage) {
			if(Exception.class.isAssignableFrom(clazz)) continue;
			if(clazz.isInterface()) continue;
			analyzeContextAnnotation(clazz);
		}
	}
	
	private void analyzeContextAnnotation(Class<?> clazz) {
		Set<String> annotationFieldName = new HashSet<String>();
		Set<String> annotationMethodName = new HashSet<String>();
		for ( ; clazz != Object.class; clazz = clazz.getSuperclass() ) {
			Field[] fieldArray = clazz.getDeclaredFields();
			for ( Field field : fieldArray ) {
				if ( annotationFieldName.contains(field.getName()) ) continue;
				if ( field.isAnnotationPresent(Dependence.class) )
					annotationFieldName.add(field.getName());
			}
			Method[] methods = clazz.getMethods();
			for ( Method method : methods ) {
				if ( annotationMethodName.contains(method.getName()) ) continue;
				if ( method.isAnnotationPresent(Initialize.class) )
					annotationFieldName.add(method.getName());
			}
		}
		contextDependenceAnnotationMapping.put(clazz.getName(), annotationFieldName);
		contextInitializeAnnotationMapping.put(clazz.getName(), annotationMethodName);
		
	}

	private final Map<String, Set<String>> contextDependenceAnnotationMapping = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> contextInitializeAnnotationMapping = new HashMap<String, Set<String>>();
	
}
