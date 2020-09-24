package pro.jk.ejoker.common.context.dev2.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.ContextRuntimeException;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.dev2.EJokerInstanceBuilder;
import pro.jk.ejoker.common.context.dev2.EjokerRootDefinationStore;
import pro.jk.ejoker.common.context.dev2.IEJokerSimpleContext;
import pro.jk.ejoker.common.context.dev2.IEjokerClazzScannerHook;
import pro.jk.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jk.ejoker.common.system.enhance.EachUtilx;
import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.helper.AcquireHelper;
import pro.jk.ejoker.common.utils.genericity.GenericDefinedField;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;
import pro.jk.ejoker.common.utils.genericity.XTypeTable;

public class EjokerContextDev2Impl implements IEjokerContextDev2 {
	
	private final static Logger logger = LoggerFactory.getLogger(EjokerContextDev2Impl.class);
	
	private final static Class<?> ConflictTypeRecord = Object.class;
	
	private final EjokerRootDefinationStore defaultRootDefinationStore = new EjokerRootDefinationStore();
	
	/**
	 * 已加载的标记集合
	 */
	private final Map<Class<?>, Object> markLoad = new HashMap<>();
	
	/**
	 * 严格映射记录<br />
	 * * 基类/接口 -&gt; eService类<br />
	 * * 要么左值和右值都没有泛型<br />
	 * * 要么左值和右值具有相同的泛型数量<br />
	 */
	private final Map<Class<?>, Class<?>> superMapperRecord = new HashMap<>();
	
	/**
	 * 是对superMapperRecord的反转关系。<br />
	 * eService类 -&gt; 基类/接口的集合<br />
	 */
	private final Map<Class<?>, Set<Class<?>>> childMapperRecord = new HashMap<>();
	
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
	private final Map<String, Object> instanceGenericTypeMap = new ConcurrentHashMap<>();
	
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
	
	Map<Integer, Queue<IVoidFunction>> initTasks = new TreeMap<>((o1, o2) -> o1.compareTo(o2));
	
	Map<Integer, Queue<IVoidFunction>> destroyTasks = new TreeMap<>((o1, o2) -> o1.compareTo(o2));
	
	private final AtomicBoolean onService = new AtomicBoolean(false);
	
	public EjokerContextDev2Impl() {

		instanceMap.put(IEJokerSimpleContext.class.getName(), this);
		instanceMap.put(IEjokerContextDev2.class.getName(), this);
		instanceMap.put(EjokerContextDev2Impl.class.getName(), this);
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> clazz) {
		
		AcquireHelper.waitAcquire(onService, true, 50, count -> {
			logger.warn("Context is not on service!!! [currentLoop: {}]", count);
		});
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(clazz);
		
		return (T )getWithExpression(genericExpress);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(TypeRefer<T> typeRef) {

		AcquireHelper.waitAcquire(onService, true, 50, count -> {
			logger.warn("Context is not on service!!! [currentLoop: {}]", count);
		});
		
		Type type = typeRef.getType();
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(type);
		
		return (T )getWithExpression(genericExpress);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Class<T> clazz, Type... types) {

		AcquireHelper.waitAcquire(onService, true, 50, count -> {
			logger.warn("Context is not on service!!! [currentLoop: {}]", count);
		});
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(clazz, types);
		
		return (T )getWithExpression(genericExpress);
	}

	@Override
	public void shallowRegister(Object instance) {
		String instanceTypeName = instance.getClass().getName();
		
		if(instanceMap.containsKey(instanceTypeName))
			throw new ContextRuntimeException("It seems another instance type of " + instanceTypeName + " has registered!!!");
		
		instanceMap.put(instanceTypeName, instance);
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
		
		if(onService.get()) {
			throw new RuntimeException("EJoker has been refresh() once!!!");
		}
		
		refreshContextRecord();
		
		EachUtilx.forEach(conflictMapperRecord, (clazz, conflictSet) -> {
			StringBuilder sb = new StringBuilder();
			for(Class<?> cClazz : conflictSet) {
				sb.append("\n\tCondidate class:\t\t");
				sb.append(cClazz.getName());
			}
			logger.warn("Conflict map relationship!\n\tUpper class:\t\t{}{}", clazz.getName(), sb);
		});
		
		preparePreviouslyLoad();
		completeInstanceInitMethod();
		
		onService.set(true);
	}
	
	@Override
	public void discard() {
		
		if(!onService.compareAndSet(true, false))
			return;
		
		
		destroyTasks.forEach((k, v) -> {
			IVoidFunction task;
			while(null != (task = v.poll())) {
				try {
					task.trigger();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		});
		
		destroyTasks.clear();
		initTasks.clear();
		
		instanceMap.clear();
		instanceGenericTypeMap.clear();
		instanceCandidateGenericTypeMap.clear();
		instanceCandidateFaildMap.clear();
		instanceCandidateDisable.clear();

		superMapperRecord.clear();
		conflictMapperRecord.clear();
		markLoad.clear();
		
	}
	
	@Override
	public void destroyRegister(IVoidFunction vf, int priority) {
		if(!onService.get()) {
			Queue<IVoidFunction> taskQueue = MapUtilx.getOrAdd(destroyTasks, priority, () -> new LinkedList<>());
			taskQueue.offer(vf);
		}
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
				
				Type[] testTypeTable = XTypeTable.getTestTypeTable(parameterizedTypeAmount);
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
							// 这里是把不带泛型签名的原类型名（全限定的） 放入 instanceCandidateDisable
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

		EachUtilx.forEach(instanceCandidateFaildMap, (expressionSignature, __) -> {
			instanceCandidateGenericTypeMap.put(expressionSignature, ConflictTypeRecord);
		});
		
		superMapperRecord.forEach((s, c) -> {
			Set<Class<?>> superSet = MapUtilx.getOrAdd(childMapperRecord, c, () -> new HashSet<>());
			superSet.add(s);
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
		
		EachUtilx.forEach(superMapperRecord, (upperClazz, eServiceClazz) -> {
			
			GenericExpression eServiceClazzMiddleStatementGenericExpression = GenericExpressionFactory.getMiddleStatementGenericExpression(eServiceClazz);
			/// 预加载
			if(!eServiceClazzMiddleStatementGenericExpression.isComplete())
				return;
			Object instance;
			if(null == (instance = instanceMap.get(eServiceClazz.getName()))) {
				instance = EJokerInstanceBuilder.doCreate(eServiceClazz, this::enqueueInitMethod);
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
			
			EachUtilx.forEach(
					defaultRootDefinationStore.getEDependenceRecord(clazz),
					(fieldName, genericDefinedField) -> injectDependence(
							fieldName,
							genericDefinedField,
							dependence -> setField(genericDefinedField.field, instance, dependence)
						)
			);
			
			
			
		});
	}
	
	private Object getWithExpression(GenericExpression genericExpress) {

		Object dependence = null;
		
		String instanceTypeName = genericExpress.expressSignature;
		
		Class<?> eServiceClazz = superMapperRecord.get(genericExpress.getDeclarePrototype());
		
		if(!genericExpress.genericDefination.hasGenericDeclare/*instanceMap.containsKey(instanceTypeName)*/) {
			/// upper无泛型 eService无泛型
			dependence = instanceMap.get(instanceTypeName);
		} else if(speculateMode && instanceCandidateGenericTypeMap.containsKey(instanceTypeName)) {
			/// upper泛型 eService无泛型
			if(!instanceCandidateFaildMap.containsKey(instanceTypeName)) {
				// 这个条件做单独判断，如果冲突导致不满足，视为失败
				eServiceClazz = instanceCandidateGenericTypeMap.get(instanceTypeName);
				if(null != eServiceClazz)
					dependence = instanceMap.get(eServiceClazz.getName());
			} else {
				logger.warn("Conflict EService!!! [requiredType: {}]", instanceTypeName);
			}
		}  else if(null != eServiceClazz && !instanceCandidateDisable.contains(instanceTypeName)) {
			/// upper泛型 eService泛型
			/// 条件表达的意思是 不存在instanceMap但是却存在于上下级映射集合中
			dependence = instanceGenericTypeMap.get(instanceTypeName);
			if(null == dependence) {
				dependence = EJokerInstanceBuilder.doCreate(
						eServiceClazz,
						null
						);
				Object previous = instanceGenericTypeMap.putIfAbsent(instanceTypeName, dependence);
				if(null != previous) {
					dependence = previous;
				} else {
					enqueueInitMethod(dependence);
					
					final Object passInstance = dependence;
					
					String signatureSuffix = instanceTypeName.substring(instanceTypeName.indexOf('<'));
					Set<Class<?>> set = childMapperRecord.get(eServiceClazz);
					if(null != set && !set.isEmpty())
						for(Class<?> s : set) {
							Object pPrevious = instanceGenericTypeMap.putIfAbsent(s.getName()+signatureSuffix, passInstance);
							if(null != pPrevious && passInstance != pPrevious) {
								throw new ContextRuntimeException(
										StringUtilx.fmt("Wrong instance mapping relationship!!! [fetchType: {}]", instanceTypeName));
							}
						}
					
					// 对新创建的EService对象注入依赖
					EachUtilx.forEach(
							defaultRootDefinationStore.getEDependenceRecord(eServiceClazz),
							(fieldName, genericDefinedField) -> injectDependence(
									fieldName,
									genericDefinedField,
									d -> setField(genericDefinedField.field, passInstance, d)
								)
					);
					
					// 对新创建的eService类执行标记的初始方法
					completeInstanceInitMethod();
					
				}
				
			}
			
			
		}
		
		if(null == dependence)
			throw new ContextRuntimeException(
					StringUtilx.fmt("No implementations or extensions found!!! [fetchType: {}]", instanceTypeName));
		
		return dependence;
	}
	
	private void injectDependence(String fieldName, GenericDefinedField genericDefinedField, IVoidFunction1<Object> effector) {
		
		if(!genericDefinedField.field.isAnnotationPresent(Dependence.class)) {
			return;
		}
		
		Object dependence = null;
		String instanceTypeName = genericDefinedField.genericDefinedType.typeName;
		
		Class<?> eServiceClazz = superMapperRecord.get(genericDefinedField.genericDefinedType.rawClazz);
		
		if(instanceMap.containsKey(instanceTypeName)) { // 在preparePreviouslyLoad()调用之后，没有泛型的eService都会被全部创建并注册
			/// upper无泛型 eService无泛型
			dependence = instanceMap.get(instanceTypeName);
		} else if(speculateMode && instanceCandidateGenericTypeMap.containsKey(instanceTypeName)) {
			/// upper泛型 eService无泛型
			if(!instanceCandidateFaildMap.containsKey(instanceTypeName)) {
				// 这个条件做单独判断，如果冲突导致不满足，视为失败
				eServiceClazz = instanceCandidateGenericTypeMap.get(instanceTypeName);
				if(null != eServiceClazz)
					dependence = instanceMap.get(eServiceClazz.getName());
			} else {
				logger.warn("Conflict EService!!! [requiredType: {}]", instanceTypeName);
			}
//			eServiceClazz = instanceCandidateGenericTypeMap.get(instanceTypeName);
//			if(null == eServiceClazz) {
//				throw new ContextRuntimeException(StringUtilx.fmt("Cound not found EService!!! [superType: {}]", instanceTypeName));
//			}
//			dependence = instanceMap.get(eServiceClazz.getName());
			
		} else if(null != eServiceClazz) {
			/// upper泛型 eService泛型
			/// 条件表达的意思是 不存在instanceMap但是却存在于上下级映射集合中
			dependence = instanceGenericTypeMap.get(instanceTypeName);
			if(null == dependence) {
				Object pervious = instanceGenericTypeMap.putIfAbsent(instanceTypeName, dependence = EJokerInstanceBuilder.doCreate(
						eServiceClazz,
						null
						));
				
				if(null != pervious) {
					dependence = pervious;
				} else {
					enqueueInitMethod(dependence);

					final Object passInstance = dependence;
					
					String signatureSuffix = instanceTypeName.substring(instanceTypeName.indexOf('<'));
					Set<Class<?>> set = childMapperRecord.get(eServiceClazz);
					if(null != set && !set.isEmpty())
						for(Class<?> s : set) {
							Object pPrevious = instanceGenericTypeMap.putIfAbsent(s.getName()+signatureSuffix, passInstance);
							if(null != pPrevious && passInstance != pPrevious) {
								throw new ContextRuntimeException(
										StringUtilx.fmt("Wrong instance mapping relationship!!! [fetchType: {}]", instanceTypeName));
							}
						}
					
					// 对新创建的EService对象注入依赖
					GenericExpression fieldTypeExpression = GenericExpressionFactory.getGenericExpressDirectly(eServiceClazz, genericDefinedField.genericDefinedType.deliveryTypeMetasTable);
					fieldTypeExpression.forEachFieldExpressionsDeeply(
							(subFieldName, subGenericDefinedField) -> injectDependence(
									subFieldName,
									subGenericDefinedField,
									subDependence -> setField(subGenericDefinedField.field, passInstance, subDependence)
								)
					);
				}
				
			};
		}
		
		if(null == dependence)
			throw new ContextRuntimeException(String.format(
							"No implementations or extensions found! \n\t field: %s#%s\n\t type: %s!!!",
							genericDefinedField.genericDefinedType.rawClazz.getName(),
							fieldName,
							instanceTypeName
							));
		
		effector.trigger(dependence);
	}
	
	private void setField(Field field, Object instance, Object dependence) {
		try {
			field.set(instance, dependence);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new ContextRuntimeException(String.format("Cannot find any dependence for field: %s !!!", field.getName()), ex);
		}
	}
	
	private void enqueueInitMethod(Object instance) {
		final Class<?> instanceClazz = instance.getClass();
		EachUtilx.forEach(
				defaultRootDefinationStore.getEInitializeRecord(instanceClazz),
				(methodName, method) -> {
					EInitialize annotation = method.getAnnotation(EInitialize.class);
					int priority = annotation.priority();
					Queue<IVoidFunction> initTaskQueue = MapUtilx.getOrAdd(initTasks, priority, () -> new LinkedBlockingQueue<>());
					initTaskQueue.offer(() -> {
						try {
							method.invoke(instance);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new ContextRuntimeException(String.format("Faild on invoke init method!!! target: %s, method: %s",
											instanceClazz.getName(), methodName),
									e);
						}
					});
				}
			);
	}
	
	private void completeInstanceInitMethod() {
		Set<Entry<Integer, Queue<IVoidFunction>>> entrySet = initTasks.entrySet();
		for(Entry<Integer, Queue<IVoidFunction>> entry : entrySet) {
			Queue<IVoidFunction> initTask = entry.getValue();
			IVoidFunction vf;
			while(null != (vf = initTask.poll()))
				vf.trigger();
		}
		initTasks.clear();
	}

}
