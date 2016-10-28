package com.jiefzz.ejoker.z.common.context.impl;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaAnalyzer;
import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaProvidor;
import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaScanner;
import com.jiefzz.ejoker.z.common.context.IEJokerContext;
import com.jiefzz.ejoker.z.common.context.IEJokerInstalcePool;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.utilities.ClassNamesScanner;

public class DefaultEJokerContext implements IEJokerContext {
	
	private DefaultEJokerClassMetaProvider eJokerClassMetaProvider = new DefaultEJokerClassMetaProvider();
	
	private IEJokerInstalcePool eJokerInstalcePool = new DefaultEJokerInstalcePool(eJokerClassMetaProvider);

	/**
	 * 主动覆盖的对象容器
	 * @deprecated 未完成
	 */
	private final Map<Class<?>, Object> coveredInstanceMap = new HashMap<Class<?>, Object>();

	/**
	 * 主动覆盖的对象容器(有泛型的)
	 * @deprecated 未完成
	 */
	private final Map<Class<?>, Map<String, Object>> coveredInstanceGenericTypeMap = new HashMap<Class<?>, Map<String, Object>>();
	
	/**
	 * 放入扫描过的包得路径的字符串
	 */
	private final Set<String> hasScanPackage = new HashSet<String>();
	
	public DefaultEJokerContext() {
		Map<Class<?>, Object> instanceMap = ((DefaultEJokerInstalcePool )eJokerInstalcePool).instanceMap;
		// 执行自构建前已存在的对象的注入
		instanceMap.put(DefaultEJokerContext.class, this);
		instanceMap.put(IEJokerContext.class, this);
		instanceMap.put(IEJokerSimpleContext.class, this);
		instanceMap.put(IEJokerClassMetaScanner.class, this);

		instanceMap.put(DefaultEJokerClassMetaProvider.class, eJokerClassMetaProvider);
		instanceMap.put(IEJokerClassMetaProvidor.class, eJokerClassMetaProvider);
		instanceMap.put(IEJokerClassMetaAnalyzer.class, eJokerClassMetaProvider);

		instanceMap.put(DefaultEJokerInstalcePool.class, eJokerInstalcePool);
		instanceMap.put(IEJokerInstalcePool.class, eJokerClassMetaProvider);
	}

	@Override
	public <T> T get(Class<T> clazz) {
		return eJokerInstalcePool.getInstance(clazz);
	}

	@Override
	public <T> T get(Class<T> clazz, String pSign) {
		return eJokerInstalcePool.getInstance(clazz, pSign);
	}

	@Override
	public void scanNamespaceClassMeta(String namespace) {
		scanPackageClassMeta(namespace);
	}

	@Override
	public void scanPackageClassMeta(String javaPackage) {

		if ( javaPackage.lastIndexOf('.') == (javaPackage.length()-1) )
			javaPackage = javaPackage.substring(0, javaPackage.length()-1);
		for ( String key : hasScanPackage )
			if(javaPackage.startsWith(key)) return; // 传入的包是某个已经被分析的包的子包或就是已存在的包，则不再分析
		hasScanPackage.add(javaPackage);
		
		List<Class<?>> scanClass;
		try {
			scanClass = ClassNamesScanner.scanClass(javaPackage);
		} catch (ClassNotFoundException e) {
			throw new ContextRuntimeException(e);
		}
		for(Class<?> clazz : scanClass) {
			// skip Throwable \ Abstract \ Interface class
			if(Throwable.class.isAssignableFrom(clazz)) continue;
			if(Modifier.isAbstract(clazz.getModifiers())) continue;
			if(clazz.isInterface()) continue;
			eJokerClassMetaProvider.analyzeClassMeta(clazz);
		}
	}

	@Override
	public <T> void regist(Object instance, Class<T> clazz) {
		Object previous = coveredInstanceMap.putIfAbsent(clazz, instance);
		if( null!=previous )
			throw new ContextRuntimeException(String.format("%s has been registed with %s before!!!", clazz.getName(), previous.getClass().getName()));
	}

	@Override
	public <T> void regist(Object instance, Class<T> clazz, String pSignature) {
		Map<String, Object> signatureCoveredInstanceMapper = coveredInstanceGenericTypeMap.getOrDefault(clazz, null);
		if( null==signatureCoveredInstanceMapper )
			coveredInstanceGenericTypeMap.put(clazz, (signatureCoveredInstanceMapper = new HashMap<String, Object>()));
		Object previous = signatureCoveredInstanceMapper.putIfAbsent(pSignature, instance);
		if( null!=previous )
			throw new ContextRuntimeException(String.format(
					"%s%s has been registed with %s%s before!!!",
					clazz.getName(), pSignature,
					previous.getClass().getName(), pSignature
			));
	}

}
