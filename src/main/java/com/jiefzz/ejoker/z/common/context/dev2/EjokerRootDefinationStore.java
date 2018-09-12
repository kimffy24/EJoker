package com.jiefzz.ejoker.z.common.context.dev2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;
import com.jiefzz.ejoker.z.common.utilities.ClassNamesScanner;
import com.jiefzz.ejoker.z.common.utilities.ForEachUtil;
import com.jiefzz.ejoker.z.common.utilities.GenericDefinedField;
import com.jiefzz.ejoker.z.common.utilities.GenericExpression;
import com.jiefzz.ejoker.z.common.utilities.GenericExpressionFactory;

public class EjokerRootDefinationStore implements IEJokerClazzScanner{
	
	private final static Logger logger = LoggerFactory.getLogger(EjokerRootDefinationStore.class);

	/**
	 * EService类里面有等待被注入的属性
	 */
	private final Map<Class<?>, Map<String, GenericDefinedField>> eDependenceMapper = new HashMap<>();

	/**
	 * EInitialize类里面有声明要初始化执行的方法
	 */
	private final Map<Class<?>, Map<String, Method>> eInitializeMapper = new HashMap<>();

	/**
	 * 记录已经分析过的middleExpression
	 */
	private final Map<Class<?>, GenericExpression> eServiceMiddleExpressions = new HashMap<>();
	
	/**
	 * 记录已经经过采集定义信息的类
	 */
	private final Set<Class<?>> hasBeenAnalyzeClass = new HashSet<>();

	private Map<Class<? extends IEjokerClazzScannerHook>, IEjokerClazzScannerHook> hookMap = 
			new ConcurrentHashMap<>();
	
	/**
	 * 放入扫描过的包得路径的字符串
	 */
	private final Set<String> hasScanPackage = new HashSet<>();
	
	/**********public method*********/
	
	public Map<String, GenericDefinedField> getEDependenceRecord(Class<?> clazz) {
		return eDependenceMapper.get(clazz);
	}
	
	public Map<String, Method> getEInitializeRecord(Class<?> clazz) {
		return eInitializeMapper.get(clazz);
	}
	
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
			if(
					Throwable.class.isAssignableFrom(clazz)
					|| Modifier.isAbstract(clazz.getModifiers())
					|| clazz.isInterface())
				continue;
			process(clazz);

			ForEachUtil.processForEach(hookMap, (hookType, hook) -> {
				hook.process(clazz);
			});
		}
	}
	
	private void process(Class<?> clazz) {

		// 已经分析过的类就跳过
		if(hasBeenAnalyzeClass.contains(clazz))
			return;
		else
			hasBeenAnalyzeClass.add(clazz);
		

		// 只关注被标记EService的类
		if(!clazz.isAnnotationPresent(EService.class))
			return;
		
		GenericExpression middleExpression = GenericExpressionFactory.getMiddleStatementGenericExpression(clazz);
		eServiceMiddleExpressions.put(clazz, middleExpression);
		
		// 同名方法在在不同的对象中的反射Method是不一样的，用方法名作唯一控制会更好
		// 分析会从子类到父类方向延伸，如果子类中出现过此方法，则会被跳过
		Map<String, Method> reflectInitializeMethodStore = new HashMap<>();
		// 同名属性在在不同的对象中的反射Field是不一样的，用属性名作唯一控制会更好
		// 分析会从子类到父类方向延伸，如果子类中出现过此方法，则会被跳过
		Map<String, GenericDefinedField> reflectDependenceGenericDefinedFieldStore = new HashMap<>();
		
		for(
				GenericExpression currentExpression = middleExpression;
				null != currentExpression && !Object.class.equals(currentExpression.getDeclarePrototype());
				currentExpression = currentExpression.getParent()
						) {
			Class<?> declaredClazz = currentExpression.getDeclarePrototype();
			
			currentExpression.forEachFieldExpressions((fieldName, genericDefinedField) -> {
				Field field = genericDefinedField.field;
				if ( field.isAnnotationPresent(Dependence.class) || field.isAnnotationPresent(Resource.class) ) {
					reflectDependenceGenericDefinedFieldStore.put(fieldName, genericDefinedField);
				}
			});
			
			Method[] declaredMethods = declaredClazz.getDeclaredMethods();
			if(null != declaredMethods) {
				for(Method method : declaredMethods) {

					// 已经添加过的方法
					if ( reflectInitializeMethodStore.containsKey(method.getName()) )
						continue;
					if ( !method.isAnnotationPresent(EInitialize.class) )
						continue;

					// TODO 默认规则： 初始化函数不传参
					/// * 带上参数后存在大量分析拆解以及上下文匹配工作，参数完全可以通过其他方法传入
					if (method.getParameterCount() > 0) {
						logger.error("Unsupport pass parameters to @Initialize method now!!! Found at {}#{}()",
								clazz.getName(), method.getName());
						throw new ContextRuntimeException("Unsupport pass parameters to @Initialize method now!!!");
					}
					reflectInitializeMethodStore.put(method.getName(), method);
				}
			}
		}
		
		eDependenceMapper.put(clazz, reflectDependenceGenericDefinedFieldStore);
		eInitializeMapper.put(clazz, reflectInitializeMethodStore);
		
	}

	@Override
	public void scanPackage(String javaPackage) {
		annotationScan(javaPackage);
	}

	@Override
	public void registeScannerHook(IEjokerClazzScannerHook hook) {
		if(null != hookMap.putIfAbsent(hook.getClass(), hook)) {
			logger.warn("Context Hook[Type: {}] has regist before! It will ignore this invokation.");
		};
	}
	
	public void forEachEServiceExpressions(IVoidFunction2<Class<?>, GenericExpression> vf) {
		ForEachUtil.processForEach(eServiceMiddleExpressions, vf);
	}
}
