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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.context.annotation.context.Initialize;
import com.jiefzz.ejoker.z.common.utilities.ClassNamesScanner;

public class RootAssemblyAnalyzer {

	private final static  Logger logger = LoggerFactory.getLogger(RootAssemblyAnalyzer.class);

	/**
	 * 记录已经经过Assembly的类
	 */
	public final List<Class<?>> hasBeenAnalyzeClass = new ArrayList<Class<?>>();
	
	/**
	 * 扫描到所有的EService类
	 */
	public final List<Class<?>> eServiceClass = new ArrayList<Class<?>>();
	
	/**
	 * 记录EService类的所有父类和继承接口。
	 */
	public final Map<Class<?>, Set<Class<?>>> eServiceHierarchyType = new HashMap<Class<?>, Set<Class<?>>>();
	
	/**
	 * 接口类型/抽象类型/实现类型与EService实现类映射对象
	 */
	public final Map<Class<?>, Class<?>> eServiceImplementationMapper = new HashMap<Class<?>, Class<?>>();
	
	/**
	 * EService类里面有等待被注入的属性
	 */
	public final Map<Class<?>, Set<Field>> eDependenceMapper = new HashMap<Class<?>, Set<Field>>();
	
	/**
	 * EService类里面有声明要初始化执行的方法
	 */
	public final Map<Class<?>, Set<Method>> eInitializeMapper = new HashMap<Class<?>, Set<Method>>();
	
	/**
	 * 带ID的EService映射
	 */
	public final Map<String, Class<?>> eServiceIdMapper = new HashMap<String, Class<?>>();
	
	/**
	 * 放入扫描过的包得路径的字符串
	 */
	public final Set<String> hasScanPackage = new HashSet<String>();
	
	/**
	 * 存入冲突的类型。
	 */
	public final Set<Class<?>> conflictResolvType = new HashSet<Class<?>>();

	public void annotationScan(String specificPackage) {
		if ( specificPackage.lastIndexOf('.') == (specificPackage.length()-1) )
			specificPackage = specificPackage.substring(0, specificPackage.length()-1);
		for ( String key : hasScanPackage )
			if(specificPackage.startsWith(key)) return;  // 传入的包是某个已经被分析的包的子包或就是已存在的包，则不再分析

		List<Class<?>> clazzInSpecificPackage;
		try {
			clazzInSpecificPackage = ClassNamesScanner.scanClass(specificPackage);
		} catch (Exception e) {
			throw new ContextRuntimeException(String.format("Exception occur whild scanning package [%s]!!!", specificPackage), e);
		}

		for (Class<?> clazz:clazzInSpecificPackage) {
			// skip Throwable \ Abstract \ Interface class
			if(Throwable.class.isAssignableFrom(clazz)) continue;
			if(Modifier.isAbstract(clazz.getModifiers())) continue;
			if(clazz.isInterface()) continue;
			analyzeContextAnnotation(clazz);
		}
	}
	
	/**
	 * DONE: 记录是否已经被分析
	 * DONE: 记录所有EService
	 * 
	 * DONE: 记录类的@Initialize方法
	 * DONE: 记录类的@Dependence/@Resource属性
	 * @param claxx
	 */
	private void analyzeContextAnnotation(final Class<?> claxx) {
		
		// 已经分析过的类就跳过
		if(hasBeenAnalyzeClass.contains(claxx)) return;
		else hasBeenAnalyzeClass.add(claxx);
		
		boolean isEService = claxx.isAnnotationPresent(EService.class);

		// collect the method which annotate by @Initialize .
		Set<Method> annotationMethodName = new HashSet<Method>();
		// collect the properties which annotate by @Dependence or @Resource .
		Set<Field> annotationFieldName = new HashSet<Field>();
		// collect the superClass or superInterface
		Set<Class<?>> resolvedHierarchyType = new HashSet<Class<?>>();
		for ( Class<?> clazz = claxx; clazz != Object.class; clazz = clazz.getSuperclass() ) {
			Method[] methods = clazz.getDeclaredMethods();
			for ( Method method : methods ) {
				if ( annotationMethodName.contains(method) ) continue;
				if ( method.isAnnotationPresent(Initialize.class) )
					annotationMethodName.add(method);
			}
			Field[] fieldArray = clazz.getDeclaredFields();
			for ( Field field : fieldArray ) {
				if ( field.isAnnotationPresent(Dependence.class) || field.isAnnotationPresent(Resource.class) )
					annotationFieldName.add(field);
			}
			
			// 收集接口映射 收集父类映射 
			Class<?>[] interfaces = clazz.getInterfaces();
			for ( Class<?> interfaceType : interfaces )
				resolvedHierarchyType.add(interfaceType);
			resolvedHierarchyType.add(clazz);
		}
		eDependenceMapper.put(claxx, annotationFieldName);
		eInitializeMapper.put(claxx, annotationMethodName);

		if(isEService) {
		eServiceHierarchyType.put(claxx, resolvedHierarchyType);
		for(Class<?> hierarchyType : resolvedHierarchyType)
			if(null!=eServiceImplementationMapper.putIfAbsent(hierarchyType, claxx)) {
				eServiceImplementationMapper.remove(hierarchyType);
				conflictResolvType.add(hierarchyType);
				logger.warn("[{}] has more than one @EService implementation!", hierarchyType.getName());
			}
		}
	}
	
}
