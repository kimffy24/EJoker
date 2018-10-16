package com.jiefzz.ejoker.z.common.context.dev2.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.dev2.EJokerInstanceBuilder;
import com.jiefzz.ejoker.z.common.context.dev2.EjokerRootDefinationStore;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerClazzScannerHook;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericDefinedField;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpression;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpressionFactory;
import com.jiefzz.ejoker.z.common.utils.genericity.XTypeTest;

public class EjokerContextDev2Impl implements IEjokerContextDev2 {
	
	private final static Logger logger = LoggerFactory.getLogger(EjokerContextDev2Impl.class);
	
	private final EjokerRootDefinationStore defaultRootDefinationStore = new EjokerRootDefinationStore();
	
	/**
	 * 已加载的标记集合
	 */
	private final Map<Class<?>, Object> markLoad = new HashMap<>();
	
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
	private final Map<Class<?>, Set<Class<?>>> conflictMapperRecord = new HashMap<>();

	/**
	 * 对象容器<br>
	 */
	private final Map<String, Object> instanceMap = new HashMap<>();

	/**
	 * 对象容器(有泛型的)
	 */
	private final Map<String, Object> instanceGenericTypeMap = new HashMap<>();
	
	/**
	 * 推演模式
	 */
	private boolean speculateMode = true;

	/**
	 * 类映射(推演模式专用)
	 */
	private final Map<String, Class<?>> instanceCandidateGenericTypeMap = new HashMap<>();
	
	/**
	 * 推演失败(推演模式专用)
	 */
	private final Map<String, Object> instanceCandidateFaildMap = new HashMap<>();
	
	/**
	 * 禁止推演记录集(推演模式专用)<br>
	 * * 针对EService是泛型 value为对应的类/接口名（带包路径的全名）
	 */
	private final Set<String> instanceCandidateDisable = new HashSet<>();
	
	Map<Integer, Queue<IVoidFunction>> initTasks = new TreeMap<>(new Comparator<Integer>() {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o1.intValue() - o2.intValue();
		}});
	
	private final AtomicBoolean onService = new AtomicBoolean(false);
	
	private final static Object defaultInstance = new Object();
	
	@Override
	public <T> T get(Class<T> clazz) {
		
		if(!onService.get())
			throw new ContextRuntimeException("context is not on service!!!");
		
		return (T )instanceMap.get(clazz.getName());
	}

	@Override
	public <T> T get(Class<T> clazz, Type... types) {
		
		if(!onService.get())
			throw new ContextRuntimeException("context is not on service!!!");
		
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
		
		assert !onService.get();
		
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
		completeInstanceInitMethod();
		onService.compareAndSet(false, true);
	}
	
	@Override
	public void discard() {
		if(!onService.get())
			return;
		
		Scavenger scavenger = this.get(Scavenger.class);
		
		markLoad.clear();
		superMapperRecord.clear();
		conflictMapperRecord.clear();
		
		instanceMap.clear();
		instanceGenericTypeMap.clear();
		instanceCandidateGenericTypeMap.clear();
		instanceCandidateFaildMap.clear();
		instanceCandidateDisable.clear();
		
		scavenger.cleanUp();
	}
	
	private void refreshContextRecord() {
		
		defaultRootDefinationStore.forEachEServiceExpressions((clazz, genericExpression) -> {
			
			Set<Class<?>> currentRecord = new HashSet<>();
			String originalExpressSignature = genericExpression.expressSignature;
			boolean eServiceIsGenericType = genericExpression.genericDefination.hasGenericDeclare;

			if (eServiceIsGenericType) {
				// 实现的EService对象是个泛型对象
				GenericExpression current;
				int parameterizedTypeAmount = genericExpression.genericDefination.getGenericDeclareAmount();
				
				Type[] testTypeTable = XTypeTest.getTestTypeTable(parameterizedTypeAmount);
				GenericExpression testGenericExpress = GenericExpressionFactory.getGenericExpress(clazz, testTypeTable);
				
				current = testGenericExpress;

				// 通过测试表达式确定其有完全相同的传递表达。
				// 不然在通过上转型加泛型声明时，无法推断子类具体泛型声明情况
				String testGenericSignature = testGenericExpress.expressSignature.substring(testGenericExpress.expressSignature.indexOf('<'));
				while (null != current && !Object.class.equals(current.getDeclarePrototype())) {
					final GenericExpression target = current;
				
					refreshContextRecordSkeleton(
							() -> target.genericDefination.hasGenericDeclare && testGenericSignature.equals(target.expressSignature.substring(target.expressSignature.indexOf('<'))),
							() -> currentRecord.add(target.getDeclarePrototype()),
							originalExpressSignature,
							target);
	
					current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
						refreshContextRecordSkeleton(
								() -> interfaceExpression.genericDefination.hasGenericDeclare && testGenericSignature.equals(interfaceExpression.expressSignature.substring(interfaceExpression.expressSignature.indexOf('<'))),
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
					current = current.getParent();
				}
			} else {
				// 实现的EService对象是个普通对象
				GenericExpression current = genericExpression;
				while (null != current && !Object.class.equals(current.getDeclarePrototype())) {
					final GenericExpression target = current;
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

							/// fix #180930
							/// 1. 存在同一个类继承多次接口的情况，如果不做前后值判断，就会自己把自己排除掉。
							Class<?> previousClazz;
							if (current.genericDefination.hasGenericDeclare && null != (previousClazz = instanceCandidateGenericTypeMap
									.putIfAbsent(current.expressSignature, clazz))) {
								if(!previousClazz.equals(clazz))
									instanceCandidateFaildMap.put(current.expressSignature, "");
							}
							current.forEachImplementationsExpressionsDeeply(interfaceExpression -> {
								/// fix #180930
								/// 1. 存在同一个类继承多次接口的情况，如果不做前后值判断，就会自己把自己排除掉。
								Class<?> previousClazzx;
								if (interfaceExpression.genericDefination.hasGenericDeclare && null != (previousClazzx = instanceCandidateGenericTypeMap
										.putIfAbsent(interfaceExpression.expressSignature, clazz))) {
									if(!previousClazzx.equals(clazz))
										instanceCandidateFaildMap.put(interfaceExpression.expressSignature, "");
								}
							});
						}
					}
					current = current.getParent();
				}
			}
			
			for(Class<?> upperClazz : currentRecord ) {
				if(Object.class.equals(upperClazz))
					continue;
				Object prevous = markLoad.putIfAbsent(upperClazz, "");
				if( null == prevous ) {
					/// 没有任何先前记录
					superMapperRecord.put(upperClazz, genericExpression.getDeclarePrototype());
				} else {
					/// 存在先前记录
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
			// TODO_complete 抽象类是个泛型，但是eservice类却不是 或者 反过来
			// ... 打印出警告信息
			logger.warn("Not match generic provide on EService and Implementation. \n\t{}\n\t{}", currentExpression.expressSignature, originalExpressSignature);
		}
	}
	
	private void preparePreviouslyLoad() {
		
		ForEachUtil.processForEach(superMapperRecord, (upperClazz, eServiceClazz) -> {
			
			GenericExpression eServiceClazzMiddleStatementGenericExpression = GenericExpressionFactory.getMiddleStatementGenericExpression(eServiceClazz);
			/// 预加载
			if(!eServiceClazzMiddleStatementGenericExpression.isComplete())
				return;
			Object instance;
			if(null == (instance = instanceMap.get(eServiceClazz.getName()))) {
				instance = (new EJokerInstanceBuilder(eServiceClazz)).doCreate(this::enqueueInitMethod);
				if(null != instanceMap.putIfAbsent(eServiceClazz.getName(), instance)) {
					instance = instanceMap.get(eServiceClazz.getName());
				}
			}
			
			instanceMap.put(upperClazz.getName(), instance);
			
		});
		
		defaultRootDefinationStore.forEachEServiceExpressions((clazz, genericExpression) -> {
			/// 预加载
			if(!genericExpression.isComplete()) {
				// throw new RuntimeException(String.format("Expect handle a complete state expression, but not!!! [ class: %s ]", clazz.getName()));
				return;
			}

			Object instance = instanceMap.get(clazz.getName());
			
			ForEachUtil.processForEach(
					defaultRootDefinationStore.getEDependenceRecord(clazz),
					(fieldName, genericDefinedField) -> injectDependence(
							fieldName,
							genericDefinedField,
							dependence -> setField(genericDefinedField.field, instance, dependence)
						)
			);
			
			
			
		});
	}
	
	private void injectDependence(String fieldName, GenericDefinedField genericDefinedField, IVoidFunction1<Object> effector) {
		
		if(!genericDefinedField.field.isAnnotationPresent(Dependence.class)) {
			return;
		}
		
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
				instanceGenericTypeMap.putIfAbsent(instanceTypeName, dependence = (new EJokerInstanceBuilder(eServiceClazz)).doCreate(
						this::enqueueInitMethod
						));
				
				{
					// 对新创建的EService对象注入依赖
					final Object passInstance = dependence;
					GenericExpression fieldTypeExpression = GenericExpressionFactory.getGenericExpress(eServiceClazz, genericDefinedField.genericDefinedTypeMeta.deliveryTypeMetasTable);
					fieldTypeExpression.forEachFieldExpressionsDeeply(
							(subFieldName, subGenericDefinedField) -> injectDependence(
									subFieldName,
									subGenericDefinedField,
									subDependence -> setField(subGenericDefinedField.field, passInstance, subDependence)
								)
					);
				}
				
			};
		} else if(speculateMode && !instanceCandidateDisable.contains(instanceTypeName)) {
			/// upper泛型 eService无泛型
			eServiceClazz = instanceCandidateGenericTypeMap.get(instanceTypeName);
			if(null == eServiceClazz) {
				throw new ContextRuntimeException(String.format("Cound not found EService for [%s]", instanceTypeName));
			}
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
		
		effector.trigger(dependence);
	}
	
	private void setField(Field field, Object instance, Object dependence) {
		try {
			field.set(instance, dependence);
		} catch (Exception e) {
			throw new ContextRuntimeException(String.format("Cannot find any dependence for field: %s !!!", field.getName()), e);
		}
	}
	
	private void enqueueInitMethod(Object instance) {
		final Class<?> instanceClazz = instance.getClass();
		ForEachUtil.processForEach(
				defaultRootDefinationStore.getEInitializeRecord(instanceClazz),
				(methodName, method) -> {
					EInitialize annotation = method.getAnnotation(EInitialize.class);
					int priority = annotation.priority();
					Queue<IVoidFunction> initTaskQueue = MapHelper.getOrAdd(initTasks, priority, LinkedBlockingQueue::new);
					initTaskQueue.offer(() -> {
							try {
								method.invoke(instance);
							} catch (Exception e) {
								throw new ContextRuntimeException(String.format("Faild on invoke init method!!! target: %s, method: %s", instanceClazz.getName(), methodName), e);
							}
						}
					);
				}
			);
	}
	
	private void completeInstanceInitMethod() {
		Set<Entry<Integer, Queue<IVoidFunction>>> entrySet = initTasks.entrySet();
		for(Entry<Integer, Queue<IVoidFunction>> entry : entrySet) {
			Queue<IVoidFunction> initTask = entry.getValue();
			while(null != initTask.peek())
				initTask.poll().trigger();
		}
	}

}
