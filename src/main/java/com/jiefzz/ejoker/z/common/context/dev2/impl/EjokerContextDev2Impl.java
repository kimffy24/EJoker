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
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;
import com.jiefzz.ejoker.z.common.utils.GenericTypeUtil;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericDefinedTypeMeta;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpression;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpressionFactory;

public class EjokerContextDev2Impl implements IEjokerContextDev2 {
	
	private final static Logger logger = LoggerFactory.getLogger(EjokerContextDev2Impl.class);
	
	private final EjokerRootDefinationStore defaultRootDefinationStore = new EjokerRootDefinationStore();
	
	/**
	 * 已加载的标记集合
	 */
	private final Map<Class<?>, Object> markLoad = new ConcurrentHashMap<>();
	
	/**
	 * 严格映射记录<br>
	 * * 基类/接口 -> eService类<br>
	 * * 要么左值和右值都没有泛型<br>
	 * * 要么左值和右值具有相同的泛型数量<br>
	 */
	private final Map<Class<?>, Class<?>> superMapperRecord = new HashMap<>();
	
	/**
	 * 冲突记录<br>
	 * * 存在多条映射路径
	 */
	private final Map<Class<?>, Set<Class<?>>> conflictMapperRecord = new ConcurrentHashMap<>();

	/**
	 * 对象容器<br>
	 */
	private final Map<String, Object> instanceMap = new ConcurrentHashMap<>();

	/**
	 * 对象容器(有泛型的)
	 */
	private final Map<String, Object> instanceGenericTypeMap = new ConcurrentHashMap<>();
	
	/**
	 * 推演模式
	 */
	private boolean speculateMode = true;

	/**
	 * 类映射(推演模式专用)
	 */
	private final Map<String, Class<?>> instanceCandidateGenericTypeMap = new ConcurrentHashMap<>();
	
	/**
	 * 推演失败(推演模式专用)
	 */
	private final Map<String, Object> instanceCandidateFaildMap = new ConcurrentHashMap<>();
	
	/**
	 * 禁止推演记录集(推演模式专用)<br>
	 * * 针对EService是泛型 value为对应的类/接口名（带包路径的全名）
	 */
	private final Set<String> instanceCandidateDisable = new HashSet<>();
	
	private final static Object defaultInstance = new Object();
	
	@Override
	public <T> T get(Class<T> clazz) {
		return (T )instanceMap.get(clazz.getName());
	}

	@Override
	public <T> T get(Class<T> clazz, Type... types) {
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(clazz, types);
		
		Object dependence = null;
		
		String instanceTypeName = genericExpress.expressSignature;
		
		Class<?> eServiceClazz = superMapperRecord.get(genericExpress.getDeclarePrototype());
		
		if(instanceMap.containsKey(instanceTypeName)) {
			/// upper无泛型 eService无泛型
			dependence = instanceMap.get(instanceTypeName);
		} else if(null != eServiceClazz) {
			/// upper泛型 eService泛型
			/// 条件表达的意思是 不存在instanceMap但是却存在于上下级映射集合中
			dependence = instanceGenericTypeMap.getOrDefault(instanceTypeName, defaultInstance);
		} else if(speculateMode && !instanceCandidateDisable.contains(instanceTypeName)) {
			/// upper泛型 eService无泛型
			eServiceClazz = instanceCandidateGenericTypeMap.get(instanceTypeName);
			dependence = instanceMap.get(eServiceClazz.getName());
		} 
		
		if(null == dependence)
			throw new ContextRuntimeException(
					String.format(
							"No implementations or extensions found! \n\t type: %s!!!",
							instanceTypeName
							));
		
		return (T )dependence;
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
	
	private void refreshContextRecord() {
		
		defaultRootDefinationStore.forEachEServiceExpressions((clazz, genericExpression) -> {
			Set<Class<?>> currentRecord = new HashSet<>();
			String originalExpressSignature = genericExpression.expressSignature;
			GenericExpression current = genericExpression;
			boolean eServiceIsGenericType = genericExpression.genericDefination.hasGenericDeclare;
			while (null != current && !Object.class.equals(current.getDeclarePrototype())) {
				final GenericExpression target = current;
				if (eServiceIsGenericType) {
					// 实现的EService对象是个泛型对象
					int parameterizedTypeAmount = genericExpression.getExportAmount();
					refreshContextRecordSkeleton(
							() -> target.genericDefination.hasGenericDeclare && 0 == parameterizedTypeAmount - target.getExportAmount(),
							() -> currentRecord.add(target.getDeclarePrototype()),
							originalExpressSignature,
							target);

					current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
						refreshContextRecordSkeleton(
								() -> interfaceExpression.genericDefination.hasGenericDeclare && 0 == parameterizedTypeAmount - target.getExportAmount(),
								() -> currentRecord.add(interfaceExpression.getDeclarePrototype()),
								originalExpressSignature,
								interfaceExpression);
					});

					{
						/// 为推演模式准备数据
						if (speculateMode) {
							instanceCandidateDisable.add(target.genericDefination.genericPrototypeClazz.getName());
							current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
								instanceCandidateDisable.add(interfaceExpression.genericDefination.genericPrototypeClazz.getName());
							});
						}
					}
				} else {
					// 实现的EService对象是个普通对象
					refreshContextRecordSkeleton(
							() -> !target.genericDefination.hasGenericDeclare,
							() -> currentRecord.add(target.getDeclarePrototype()),
							originalExpressSignature,
							target);

					current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
						refreshContextRecordSkeleton(
								() -> !interfaceExpression.genericDefination.hasGenericDeclare,
								() -> currentRecord.add(interfaceExpression.getDeclarePrototype()),
								originalExpressSignature,
								interfaceExpression);
					});

					{
						/// 为推演模式准备数据
						if (speculateMode) {
							if (current.genericDefination.hasGenericDeclare && null != instanceCandidateGenericTypeMap
									.putIfAbsent(current.expressSignature, clazz)) {
								instanceCandidateFaildMap.put(current.expressSignature, "");
							}
							current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
								if (interfaceExpression.genericDefination.hasGenericDeclare && null != instanceCandidateGenericTypeMap
										.putIfAbsent(interfaceExpression.expressSignature, clazz)) {
									instanceCandidateFaildMap.put(interfaceExpression.expressSignature, "");
								}
							});
						}
					}
				}
				current = current.getParent();
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

		ForEachUtil.processForEach(instanceCandidateFaildMap, (expressionSignature, nonce) -> {
			instanceCandidateGenericTypeMap.remove(expressionSignature);
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
	
	private void preparePreviouslyLoad() {
		
		ForEachUtil.processForEach(superMapperRecord, (upperClazz, eServiceClazz) -> {
			
			GenericExpression eServiceClazzMiddleStatementGenericExpression = GenericExpressionFactory.getMiddleStatementGenericExpression(eServiceClazz);
			/// TODO 预加载
			if(!eServiceClazzMiddleStatementGenericExpression.isComplete())
				return;
			Object instance;
			if(null == (instance = instanceMap.get(eServiceClazz.getName()))) {
				instance = (new EJokerInstanceBuilder(eServiceClazz)).doCreate();
				if(null != instanceMap.putIfAbsent(eServiceClazz.getName(), instance)) {
					instance = instanceMap.get(eServiceClazz.getName());
				}
			}
			instanceMap.put(upperClazz.getName(), instance);
			
		});
		
		defaultRootDefinationStore.forEachEServiceExpressions((clazz, genericExpression) -> {
			/// TODO 预加载
			if(!genericExpression.isComplete())
				return;

			Object instance = instanceMap.get(clazz.getName());
			
			ForEachUtil.processForEach(defaultRootDefinationStore.getEDependenceRecord(clazz), (fieldName, genericDefinedField) -> {
				Object dependence = null;
				
				String instanceTypeName = genericDefinedField.genericDefinedTypeMeta.typeName;
				
				Class<?> eServiceClazz = superMapperRecord.get(genericDefinedField.genericDefinedTypeMeta.rawClazz);
				
				if(instanceMap.containsKey(instanceTypeName)) {
					/// upper无泛型 eService无泛型
					dependence = instanceMap.get(instanceTypeName);
				} else if(null != eServiceClazz) {
					/// upper泛型 eService泛型
					/// 条件表达的意思是 不存在instanceMap但是却存在于上下级映射集合中
					if(defaultInstance.equals(dependence = instanceGenericTypeMap.getOrDefault(instanceTypeName, defaultInstance))) {
						instanceGenericTypeMap.putIfAbsent(instanceTypeName, dependence = (new EJokerInstanceBuilder(eServiceClazz)).doCreate());
					};
				} else if(speculateMode && !instanceCandidateDisable.contains(instanceTypeName)) {
					/// upper泛型 eService无泛型
					eServiceClazz = instanceCandidateGenericTypeMap.get(instanceTypeName);
					dependence = instanceMap.get(eServiceClazz.getName());
					
				} 
				
				if(null == dependence)
					throw new ContextRuntimeException(
							String.format(
									"No implementations or extensions found! \n\t field: %s#%s\n\t type: %s!!!",
									genericDefinedField.genericDefinedTypeMeta.rawClazz.getName(),
									fieldName,
									instanceTypeName
									));
				
				try {
					genericDefinedField.field.setAccessible(true);
					genericDefinedField.field.set(instance, dependence);
				} catch (Exception e) {
					throw new ContextRuntimeException(String.format("Cannot find any dependence for field: %s !!!", genericDefinedField.field.getName()), e);
				}
			});
			
		});
	}
	
}
