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
import com.jiefzz.ejoker.z.common.utilities.GenericDefinedTypeMeta;
import com.jiefzz.ejoker.z.common.utilities.GenericExpression;
import com.jiefzz.ejoker.z.common.utilities.GenericExpressionFactory;
import com.jiefzz.ejoker.z.common.utilities.GenericTypeUtil;

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
	 * 已创建的标记集合
	 */
//	private final Map<Class<?>, Object> markCreate = new ConcurrentHashMap<>();
	
	/**
	 * 对象容器<br>
	 */
	private final Map<String, Object> instanceMap = new ConcurrentHashMap<>();

	/**
	 * 对象容器(有泛型的)
	 */
	private final Map<String, Object> instanceGenericTypeMap = new ConcurrentHashMap<>();
	
	private final Lock genericInstanceCreateLock = new ReentrantLock();
	
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
					// throw new ContextRuntimeException();
					// return;
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
				
				GenericDefinedTypeMeta fieldDefinedTypeMeta = genericDefinedField.genericDefinedTypeMeta;
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
				
//				if(genericDefinedField.isGenericVariable) {
//					/// 当前的属性是类型是个泛型声明
//					System.err.println(123);
//					throw new RuntimeException();
//				} else {
//					/// 当前的属性是个明确类型声明
//					Class<?> upperClazz = genericDefinedField.genericDefinedTypeMeta.rawClazz;
//					String upperClazzName = upperClazz.getName();
//					
//					if(GenericTypeUtil.ensureClassIsGenericType(upperClazz)) {
//						/// 明确类型 - 带泛型声明的  （有不带泛型声明的情况，但是应该活在refresh过程中报出异常）
//						/// 1. 从上下级映射集合中查找EService类
//						Class<?> eServiceClazz = superMapperRecord.get(upperClazz);
//						
//						/// 2
//						if(null != eServiceClazz) {
//							/// 2.1 如果找到泛型映射
//						} else {
//							/// 2.2 推演模式 & 当前上级类不在{禁止推演集合}中 & 推演记录在取出锚定EService类不为空 eServiceClazz 赋值为此 锚定类
//							/// 2.2 如果不满足上述条件，则抛出异常
//							if(
//									speculateMode &&
//									!instanceCandidateDisable.contains(upperClazzName) &&
//									null != (eServiceClazz = instanceCandidateGenericTypeMap.get(genericDefinedField.genericDefinedTypeMeta.typeName))
//									) {
//								
//								
//							} else {
//								throw new ContextRuntimeException(
//									String.format(
//											"No implementations or extensions found! \n\t field: %s#%s\n\t type: %s!!!",
//											upperClazzName,
//											fieldName,
//											genericDefinedField.genericDefinedTypeMeta.rawClazz.getName()
//											));
//							}
//						}
//						
//						/// 3. 取出EService类的泛型表达 （此处不会取出不完全态的表达 因为开始就排除了定义类的非完全态）
//						GenericExpression genericFieldTypeExpress = GenericExpressionFactory.getGenericExpress(
//								eServiceClazz,
//								genericDefinedField.genericDefinedTypeMeta.deliveryTypeMetasTable);
//						
//						String genericFieldExpressionSignature = genericFieldTypeExpress.expressSignature;
//						
//						if(null == (dependence = instanceGenericTypeMap.get(genericFieldExpressionSignature))) {
//							dependence = getOrcreateGenericInstance(genericFieldTypeExpress);
//						}
//						
//					} else {
//						/// 明确类型 - 不带泛型声明的
//						dependence = instanceMap.get(upperClazzName);
//						
//					}
//					
//				}
				
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
