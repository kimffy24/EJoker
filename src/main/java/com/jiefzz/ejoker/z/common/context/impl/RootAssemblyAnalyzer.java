
package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import com.jiefzz.ejoker.z.common.utilities.GeneralTypeUtil;

public class RootAssemblyAnalyzer {

	private final static  Logger logger = LoggerFactory.getLogger(RootAssemblyAnalyzer.class);

	/**
	 * 记录已经经过Assembly的类
	 */
	public final Set<Class<?>> hasBeenAnalyzeClass = new HashSet<Class<?>>();

	/**
	 * 扫描到所有的EService类
	 */
	public final Set<Class<?>> eServiceClass = new HashSet<Class<?>>();

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
	
	/**
	 * 声明者与实现者泛型签名不严格对称的接口类或抽象类
	 */
	public final Set<Class<?>> ambiguousSignatureGeneralType = new HashSet<Class<?>>();

	public void annotationScan(String specificPackage) {
		if ( specificPackage.lastIndexOf('.') == (specificPackage.length()-1) )
			specificPackage = specificPackage.substring(0, specificPackage.length()-1);
		for ( String key : hasScanPackage )
			if(specificPackage.startsWith(key)) return;// 传入的包是某个已经被分析的包的子包或就是已存在的包，则不再分析

		List<Class<?>> clazzInSpecificPackage;
		try {
			clazzInSpecificPackage = ClassNamesScanner.scanClass(specificPackage);
			hasScanPackage.add(specificPackage);
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
		if(!isEService) return;

		// 同名方法在在不同的对象中的反射Method是不一样的，用方法名作唯一控制会更好
		Map<String, Object> conflictMethodNames = new HashMap<String, Object>();
		// 同名属性在在不同的对象中的反射Field是不一样的，用属性名作唯一控制会更好
		Map<String, Object> conflictFieldNames = new HashMap<String, Object>();

		// collect the method which annotate by @Initialize .
		Set<Method> annotationMethods = new HashSet<Method>();
		// collect the properties which annotate by @Dependence or @Resource .
		Set<Field> annotationFields = new HashSet<Field>();
		// collect the superClass or superInterface
		Set<Class<?>> resolvedHierarchyType = new HashSet<Class<?>>();

		for ( Class<?> clazz = claxx; clazz != Object.class; clazz = clazz.getSuperclass() ) {

			// 扫描所有被标注为初始化的方法
			Method[] methods = clazz.getDeclaredMethods();
			for ( Method method : methods ) {
				if ( conflictMethodNames.containsKey(method.getName()) ) continue;
				if ( method.isAnnotationPresent(Initialize.class) ) {
					annotationMethods.add(method);
					conflictMethodNames.put(method.getName(), method);
				}
			}

			// 扫描所有标记为依赖的属性
			Field[] fieldArray = clazz.getDeclaredFields();
			for ( Field field : fieldArray ) {
				if ( field.isAnnotationPresent(Dependence.class) || field.isAnnotationPresent(Resource.class) )
					if(!conflictFieldNames.containsKey(field.getName())) { 
						annotationFields.add(field);
						conflictFieldNames.put(field.getName(), field);
					}
			}

			// 收集接口映射 收集父类映射 
			Class<?>[] interfaces = clazz.getInterfaces();
			for ( Class<?> interfaceType : interfaces )
				if(!resolvedHierarchyType.contains(interfaceType))
					resolvedHierarchyType.add(interfaceType);

			resolvedHierarchyType.add(clazz);
		}
		eDependenceMapper.put(claxx, annotationFields);
		eInitializeMapper.put(claxx, annotationMethods);
		eServiceHierarchyType.put(claxx, resolvedHierarchyType);
		
		for(Class<?> hierarchyType : resolvedHierarchyType) {

			// 严格检查泛型签名抽象类/接口与实现类对称
			if(!GeneralTypeUtil.getClassDefinationGeneralSignature(claxx).equals(GeneralTypeUtil.getClassDefinationGeneralSignature(hierarchyType))) {
				if(!ambiguousSignatureGeneralType.contains(hierarchyType)) ambiguousSignatureGeneralType.add(hierarchyType);
				logger.warn("Unmatch GeneralSignature: \n\t{}\t\t{}\n\t{}\t\t{}",
						hierarchyType.getName(), GeneralTypeUtil.getClassDefinationGeneralSignature(hierarchyType),
						claxx.getName(), GeneralTypeUtil.getClassDefinationGeneralSignature(claxx)
				);
			}
			
			if(conflictResolvType.contains(hierarchyType)) {
				// 如果冲突集合中有该抽象类或接口的记录，则给出警告，并跳过。
				warnningLogger(hierarchyType, claxx);
				continue;
			}
			Class<?> previousResolvedType=eServiceImplementationMapper.putIfAbsent(hierarchyType, claxx);
			if(null!=previousResolvedType) {
				// 清理EService映射，并记录conflict
				conflictResolvType.add(hierarchyType);
				eServiceImplementationMapper.remove(hierarchyType);
				warnningLogger(hierarchyType, previousResolvedType);
				warnningLogger(hierarchyType, claxx);
			}
		}
	}
	
	private void warnningLogger(Class<?> superTypeName, Class<?> implementationTypeName) {
		logger.warn("[{}] has more than one @EService implementation!", superTypeName.getName());
		logger.warn("[{}] can not be resolve to [{}]", superTypeName.getName(), implementationTypeName.getName());
	}
}
