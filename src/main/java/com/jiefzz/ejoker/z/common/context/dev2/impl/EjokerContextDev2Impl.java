package com.jiefzz.ejoker.z.common.context.dev2.impl;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.dev2.EJokerInstanceBuilder;
import com.jiefzz.ejoker.z.common.context.dev2.EjokerRootDefinationStore;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerClazzScannerHook;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.utilities.ForEachUtil;
import com.jiefzz.ejoker.z.common.utilities.GenericExpression;
import com.jiefzz.ejoker.z.common.utilities.GenericExpressionFactory;
import com.jiefzz.ejoker.z.common.utilities.GenericTypeUtil;

public class EjokerContextDev2Impl implements IEjokerContextDev2 {
	
	private final static Logger logger = LoggerFactory.getLogger(EjokerContextDev2Impl.class);
	
	private final EjokerRootDefinationStore defaultRootDefinationStore = new EjokerRootDefinationStore();
	
	private final Map<Class<?>, Object> markLoad = new ConcurrentHashMap<>();
	
	private final Map<Class<?>, Class<?>> superMapperRecord = new HashMap<>();
	
	private final Map<Class<?>, Set<Class<?>>> conflictMapperRecord = new ConcurrentHashMap<>();

	private final Map<Class<?>, Object> markCreate = new ConcurrentHashMap<>();
	
	/**
	 * 对象容器<br>
	 * ** public是为了让DefaultEJokerContext实现自注入用。
	 */
	private final Map<String, Object> instanceMap = new ConcurrentHashMap<>();

	/**
	 * 对象容器(有泛型的)
	 */
	private final Map<String, Object> instanceGenericTypeMap = new ConcurrentHashMap<>();
	
	private final Lock genericInstanceCreateLock = new ReentrantLock();
	
	@Override
	public <T> T get(Class<T> clazz) {
		return (T )instanceMap.get(clazz.getName());
	}

	@Override
	public <T> T get(Class<T> clazz, Type... types) {
		return null;
	}

	@Override
	public void scanPackage(String javaPackage) {
		defaultRootDefinationStore.scanPackage(javaPackage);
	}

	@Override
	public void registeScannerHook(IEjokerClazzScannerHook hook) {
		defaultRootDefinationStore.registeScannerHook(hook);
	}

	@Override
	public void refresh() {
		refreshContextRecord();
		
		ForEachUtil.processForEach(conflictMapperRecord, (clazz, conflictSet) -> {
			StringBuilder sb = new StringBuilder();
			for(Class<?> cClazz : conflictSet) {
				sb.append("\n\tCondidate class:\t\t");
				sb.append(cClazz.getName());
			}
			logger.warn("Conflict map relationship!\n\tUpper class:\t\t{}{}", clazz.getName(), sb);
		});
		
		preparePreviouslyLoad();
	}
	
	private void preparePreviouslyLoad() {
		
		ForEachUtil.processForEach(superMapperRecord, (upperClazz, eServiceClazz) -> {
			GenericExpression eServiceClazzMiddleStatementGenericExpression = GenericExpressionFactory.getMiddleStatementGenericExpression(eServiceClazz);
			/// TODO 预加载
			if(!eServiceClazzMiddleStatementGenericExpression.isComplete())
				return;
			Object instance;
			if(null == markCreate.putIfAbsent(eServiceClazz, "")) {
				instance = (new EJokerInstanceBuilder(eServiceClazz)).doCreate();
				if(null != instanceMap.putIfAbsent(eServiceClazz.getName(), instance)) {
					throw new ContextRuntimeException();
				}
				instanceMap.put(upperClazz.getName(), instance);
			} else {
				instance = instanceMap.get(eServiceClazz.getName());
				instanceMap.put(upperClazz.getName(), instance);
			}
			
		});
		
		defaultRootDefinationStore.forEachEServiceExpressions((clazz, genericExpression) -> {
			/// TODO 预加载
			if(!genericExpression.isComplete())
				return;

			Object instance = instanceMap.get(clazz.getName());
			
			ForEachUtil.processForEach(defaultRootDefinationStore.getEDependenceRecord(clazz), (fieldName, genericDefinedField) -> {
				Object dependence;
				if(genericDefinedField.isGenericVariable) {
					
					System.err.println(123);
					throw new RuntimeException();
					
				} else {
					
					if(GenericTypeUtil.ensureClassIsGenericType(genericDefinedField.genericDefinedTypeMeta.rawType)) {

						GenericExpression genericFieldTypeExpress = GenericExpressionFactory.getGenericExpress(
								genericDefinedField.genericDefinedTypeMeta.rawType,
								genericDefinedField.genericDefinedTypeMeta.deliveryTypeMetasTable);
						
						String genericFieldExpressionSignature = genericFieldTypeExpress.expressSignature;
						
						if(null == (dependence = instanceGenericTypeMap.get(genericFieldExpressionSignature))) {
							dependence = getOrcreateGenericInstance(genericFieldTypeExpress);
						}
						
					} else {
						
						dependence = instanceMap.get(genericDefinedField.genericDefinedTypeMeta.rawType.getName());
						
					}
					
				}
				
				if(null == dependence) {
					throw new ContextRuntimeException(String.format("Cannot find any dependence for field: %s !!!", genericDefinedField.field.getName()));
				}
				
				try {
					genericDefinedField.field.setAccessible(true);
					genericDefinedField.field.set(instance, dependence);
				} catch (Exception e) {
					throw new ContextRuntimeException(String.format("Cannot find any dependence for field: %s !!!", genericDefinedField.field.getName()), e);
				}
			});
			
//			Class<?> resolvedClass = genericExpression.getDeclarePrototype();
//			Object instance = null;
//			{
//				instance = (new EJokerInstanceBuilder(resolvedClass)).doCreate();
//				{ // 注册到泛型对象记录变量 instanceGenericTypeMap
////					instanceGenericTypeMap.get(clazz).put(pSign, instance);
//				}
//				if(!GenericTypeUtil.ensureClassIsGenericType(resolvedClass)) {
//					// 如果解析类型并不是泛型，则同时注册到非泛型容器中。
////					instanceMap.putIfAbsent(resolvedClass, instance);
//				}
//				{ // 注入依赖
////					assembling(instance);
//				}
//				{ // 执行EInitialize标记的方法
////					eJokerClassMetaProvider.executeEInitialize(resolvedClass, instance);
//				}
//			}
//			{
//				// 注册到泛型对象记录变量 instanceGenericTypeMap
////				instanceGenericTypeMap.get(clazz).put(pSign, instance);
//			}
			
		});
	}
	
	private void refreshContextRecord() {
		
		defaultRootDefinationStore.forEachEServiceExpressions((clazz, genericExpression) -> {
			Set<Class<?>> currentRecord = new HashSet<>();
			String originalExpressSignature = genericExpression.expressSignature;
			if(genericExpression.meta.isGenericType) {
				// 实现的EService对象是个泛型对象
				int parameterizedTypeAmount = genericExpression.getExportAmount();
				GenericExpression current = genericExpression;
				while(null != current && !Object.class.equals(current.getDeclarePrototype())) {
					final GenericExpression target = current;
					refreshContextRecordSkeleton(
							() -> target.meta.isGenericType && 0 == parameterizedTypeAmount - target.getExportAmount(),
							() -> currentRecord.add(target.getDeclarePrototype()),
							originalExpressSignature,
							target);
					
					current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
						refreshContextRecordSkeleton(
								() -> interfaceExpression.meta.isGenericType && 0 == parameterizedTypeAmount - target.getExportAmount(),
								() -> currentRecord.add(interfaceExpression.getDeclarePrototype()),
								originalExpressSignature,
								interfaceExpression);
					});
					current = current.getParent();
				}
			} else {
				// 实现的EService对象是个普通对象
				GenericExpression current = genericExpression;
				while(null != current && !Object.class.equals(current.getDeclarePrototype())) {
					final GenericExpression target = current;
					refreshContextRecordSkeleton(
							() -> !target.meta.isGenericType,
							() -> currentRecord.add(target.getDeclarePrototype()),
							originalExpressSignature,
							target);
					
					current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
						refreshContextRecordSkeleton(
								() -> !interfaceExpression.meta.isGenericType,
								() -> currentRecord.add(interfaceExpression.getDeclarePrototype()),
								originalExpressSignature,
								interfaceExpression);
					});
					current = current.getParent();
				}
			}
			
			for(Class<?> upperClazz : currentRecord ) {
				if(Object.class.equals(upperClazz))
					continue;
				Object prevous = markLoad.putIfAbsent(upperClazz, "");
				if( null == prevous ) {
					// TODO 没有任何先前记录
					superMapperRecord.put(upperClazz, genericExpression.getDeclarePrototype());
				} else {
					// TODO 存在先前记录
					Set<Class<?>> conflictSet;
					if(null == (conflictSet = conflictMapperRecord.get(upperClazz))) {
						Set<Class<?>> putIfAbsent = conflictMapperRecord.putIfAbsent(upperClazz, conflictSet = new HashSet<>());
						if(null != putIfAbsent)
							conflictSet = putIfAbsent;
					}
					conflictSet.add(genericExpression.getDeclarePrototype());
					Class<?> prevousRecord = superMapperRecord.remove(upperClazz);
					if(null != prevousRecord)
						conflictSet.add(prevousRecord);
				}
			}
		});
	}
	
	private void refreshContextRecordSkeleton(
			IFunction<Boolean> checker,
			IVoidFunction effect,
			String originalExpressSignature,
			GenericExpression currentExpression
			) {
		if(checker.trigger()) {
			effect.trigger();
		} else {
			// TODO 抽象类是个泛型，但是eservice类却不是 或者 反过来
			// ... 打印出警告信息
			logger.warn("Not match generic provide on EService and Implementation. \n\t{}\n\t{}", currentExpression.expressSignature, originalExpressSignature);
		}
	}

	private Object getOrcreateGenericInstance(GenericExpression genericExpression) {
		genericInstanceCreateLock.lock();
		try {
			Object prevous = instanceGenericTypeMap.get(genericExpression.expressSignature);
			if(null != prevous)
				return prevous;
			
			Class<?> instanceClazz = genericExpression.getDeclarePrototype();
			final Object instance = (new EJokerInstanceBuilder(genericExpression.getDeclarePrototype())).doCreate();
			
			{
				GenericExpression current = genericExpression;
				while(null != current && !Object.class.equals(current.getDeclarePrototype())) {
					GenericExpression target = current;
					
					if(instanceClazz.equals(superMapperRecord.get(target.getDeclarePrototype()))) {
						instanceGenericTypeMap.put(target.expressSignature, instance);
					}
					
					current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
						if(instanceClazz.equals(superMapperRecord.get(target.getDeclarePrototype()))) {
							instanceGenericTypeMap.put(target.expressSignature, instance);
						}
					});
					
					current = current.getParent();
				}
			}
			
			return instance;
			
		} finally {
			genericInstanceCreateLock.unlock();
		}
	}
	
}
