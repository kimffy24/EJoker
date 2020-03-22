package pro.jiefzz.ejoker.common.utils.relationship;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.context.annotation.persistent.PersistentIgnore;
import pro.jiefzz.ejoker.common.system.enhance.EachUtilx;
import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.helper.Ensure;
import pro.jiefzz.ejoker.common.utils.GenericTypeUtil;
import pro.jiefzz.ejoker.common.utils.SerializableCheckerUtil;
import pro.jiefzz.ejoker.common.utils.genericity.GenericDefinedType;
import pro.jiefzz.ejoker.common.utils.genericity.GenericExpression;
import pro.jiefzz.ejoker.common.utils.genericity.GenericExpressionFactory;

/**
 * 对象关系二维化工具类
 * 
 * @author kimffy
 *
 * @param <ContainerKVP>
 * @param <ContainerVP>
 */
public class RelationshipTreeUtil<ContainerKVP, ContainerVP> extends AbstractRelationshipUtil{

	private final static Logger logger = LoggerFactory.getLogger(RelationshipTreeUtil.class);

	private final IRelationshipTreeAssemblers<ContainerKVP, ContainerVP> eval;
	
	public RelationshipTreeUtil(IRelationshipTreeAssemblers<ContainerKVP, ContainerVP> eval,
			SpecialTypeCodecStore<?> specialTypeCodecStore) {
		super(specialTypeCodecStore);
		this.eval = eval;
		Ensure.notNull(eval, "RelationshipTreeUtil.eval");
	}
	public RelationshipTreeUtil(IRelationshipTreeAssemblers<ContainerKVP, ContainerVP> eval) {
		this(eval, null);
	}
	
	public ContainerKVP getTreeStructure(Object target) {
		
		Queue<IVoidFunction> queue = new LinkedBlockingQueue<>();
		
		Class<?> targetClazz = target.getClass();
		if(SerializableCheckerUtil.hasSublevel(targetClazz)) {
			throw new RuntimeException("Unsupport getTreeStructure() action on java collection util!!!");
		}
		
		if(GenericTypeUtil.ensureClassIsGenericType(targetClazz)) {
			throw new RuntimeException("Unsupport getTreeStructure() while top node genericity type");
		};
		
		GenericExpression targetExpression = GenericExpressionFactory.getGenericExpress(targetClazz);
		ContainerKVP createNode = eval.createKeyValueSet();
		targetExpression.forEachFieldExpressionsDeeply(
				(fieldName, genericDefinedField) -> join(() -> {
					
					if(checkIgnoreField(genericDefinedField.field))
						return;
					
					Object fieldValue;
					try {
						fieldValue = genericDefinedField.field.get(target);
					} catch (IllegalArgumentException|IllegalAccessException e) {
						logger.error("Cannot access field!!!", e);
						throw new RuntimeException(e);
					}
					assemblyStructure(
							genericDefinedField.genericDefinedType,
							fieldValue,
							(result) -> eval.addToKeyValueSet(createNode, result, fieldName),
							() -> fieldName,
							queue
					);
					
				}, queue));
		IVoidFunction task;
		while(null != (task = queue.poll())) {
			task.trigger();
		};
		
		return createNode;
	}
	
	private void assemblyStructure(GenericDefinedType targetDefinedTypeMeta, Object target, IVoidFunction1<Object> effector, IFunction<String> keyAccesser, Queue<IVoidFunction> subTaskQueue) {
		
		if(null == target) {
			effector.trigger(null);
			return;
		}
		
		final Class<?> valueClazz = target.getClass();
		final Class<?> definedClazz = targetDefinedTypeMeta.rawClazz;
		final String key = keyAccesser.trigger();
		
		if(!targetDefinedTypeMeta.hasGenericDeclare && GenericTypeUtil.ensureClassIsGenericType(valueClazz)) {
				throw new RuntimeException("Unmatch genericity signature!!!");
		}
		
		Object node;
		if (null != (node = processWithUserSpecialCodec(target, definedClazz))) {
//			if (!ParameterizedTypeUtil.isDirectSerializableType(node.getClass())) {
//				String errmsg = String.format("Get an unexpect type from userSpecialCodec!!! targetClass: %s, resultClass: %s, occur on: %s", definedClazz.getName(), node.getClass().getName(), key);
//				throw new RuntimeException(errmsg);
//			}
		} else if (SerializableCheckerUtil.isDirectSerializableType(definedClazz)) {
			// 属性定义为基础类型 或 属性定义为泛型但是值是基础类型
			node = target;
		} else if (definedClazz.isEnum()) {
			// 枚举类型
			node = ((Enum<?> )target).name();
		} else if (SerializableCheckerUtil.hasSublevel(definedClazz)) {
			// Java集合类型
			if (target instanceof Queue) {
				if(!target.getClass().getSimpleName().endsWith("List"))
					throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
			}
			if (target instanceof Collection) {
				ContainerVP createValueSet = eval.createValueSet();
				node = createValueSet;
				EachUtilx.forEach((List )target, item ->
					join( () ->
						assemblyStructure(
								targetDefinedTypeMeta.deliveryTypeMetasTable[0],
								item,
								(result) -> eval.addToValueSet(createValueSet, result),
								() -> key + "(#foreach in list)",
								subTaskQueue
						),
						subTaskQueue
					)
				);
			} else if (target instanceof Map) {
				GenericDefinedType pass1TypeMeta = targetDefinedTypeMeta.deliveryTypeMetasTable[0];
				if(!SerializableCheckerUtil.isDirectSerializableType(pass1TypeMeta.rawClazz))
					throw new RuntimeException(
							StringUtilx.fill(
									"We just support java base data type on the key while opera in serializing map!!! [map.keyType: {}, field: {}#{}]",
									pass1TypeMeta.rawClazz.getSimpleName(),
									targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
									key));
				
				GenericDefinedType pass2TypeMeta = targetDefinedTypeMeta.deliveryTypeMetasTable[1];
				ContainerKVP createNode = eval.createKeyValueSet();
				node = createNode;
				EachUtilx.forEach((Map )target, (k, v) -> {
						join(() -> assemblyStructure(
								pass2TypeMeta,
								v,
								(result) -> eval.addToKeyValueSet(createNode, result, k.toString()),
								() -> k.toString(),
								subTaskQueue),
							subTaskQueue);
					}
				);
			}
		} else if (targetDefinedTypeMeta.isArray) {
			// 数组类型
			ContainerVP createValueSet = eval.createValueSet();
			node = createValueSet;
			
			if(definedClazz.isPrimitive()) {
				join(()-> privateTypeForEach(target, definedClazz, createValueSet), subTaskQueue);
			} else {
				Object[] objArray = (Object[])target;
				for(int i=0; i<objArray.length; i++) {
					Object item = objArray[i];
					int index = i;
					join( () -> 
						assemblyStructure(
								targetDefinedTypeMeta.componentTypeMeta,
								objArray[index],
								(result) -> eval.addToValueSet(createValueSet, result),
								() -> key + index,
								subTaskQueue
						),
						subTaskQueue
					);
				};
			}
			
		} else {
			{
				/// 不支持部分数据类型。
				if (UnsupportTypes.isUnsupportType(valueClazz)) {
					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on %s#%s", valueClazz.getName(),
							targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
							key));
				}
				if (UnsupportTypes.isUnsupportType(definedClazz)) {
					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on %s#%s", definedClazz.getName(),
							targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
							key));
				}
			}
			/// 普通对象类型
			ContainerKVP createNode = eval.createKeyValueSet();
			node = createNode;
			GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(definedClazz,
					targetDefinedTypeMeta.deliveryTypeMetasTable);
			genericExpress.forEachFieldExpressionsDeeply(
					(fieldName, genericDefinedField) -> join(() -> {
						
						if(checkIgnoreField(genericDefinedField.field))
							return;
						
						Object fieldValue;
						try {
							fieldValue = genericDefinedField.field.get(target);
						} catch (IllegalArgumentException|IllegalAccessException e) {
							logger.error("Cannot access field!!!", e);
							throw new RuntimeException(e);
						}
						assemblyStructure(
								genericDefinedField.genericDefinedType,
								fieldValue,
								(result) -> eval.addToKeyValueSet(createNode, result, fieldName),
								() -> fieldName,
								subTaskQueue
						);
						
					}, subTaskQueue));
		}
		// 当作普通对象处理
		effector.trigger(node);
	}
	
	private void join(IVoidFunction task, Queue<IVoidFunction> subTaskQueue) {
		if(!subTaskQueue.offer(task)) {
			throw new RuntimeException("Task Queue has no more capacity!!!");
		}
	}

	/**
	 * 尽量减少装箱操作。<br>
	 * * 皆因java不支持基数类型的泛型
	 * @param valueSet
	 * @param componentType
	 * @param object
	 */
	private void privateTypeForEach(Object privateArray, Class<?> componentType, ContainerVP valueSet) {
		if (long.class == componentType) {
			// long
			long[] pArray = (long[] )privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (int.class == componentType) {
			// integer
			int[] pArray = (int[] )privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (short.class == componentType) {
			// short
			short[] pArray = (short[])privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (double.class == componentType) {
			// double
			double[] pArray = (double[])privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (float.class == componentType) {
			// float
			float[] pArray = (float[])privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (char.class == componentType) {
			// char
			char[] pArray = (char[])privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (byte.class == componentType) {
			// byte
			byte[] pArray = (byte[])privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else if (boolean.class == componentType) {
			// boolean
			boolean[] pArray = (boolean[])privateArray;
			for (int i = 0; i< pArray.length; i++)
				eval.addToValueSet(valueSet, pArray[i]);
		} else {
			// this should never happen!!!
			throw new RuntimeException();
		}
	}
	
	public static boolean checkIgnoreField(Field field) {
		int modifiers = field.getModifiers();
		return (
				field.isAnnotationPresent(PersistentIgnore.class)
				|| Modifier.isStatic(modifiers)
				|| Modifier.isFinal(modifiers)
		);
	}
}
