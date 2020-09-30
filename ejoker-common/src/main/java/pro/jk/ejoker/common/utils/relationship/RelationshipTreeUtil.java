package pro.jk.ejoker.common.utils.relationship;

import static pro.jk.ejoker.common.system.enhance.StringUtilx.fmt;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.system.enhance.EachUtilx;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.helper.Ensure;
import pro.jk.ejoker.common.utils.GenericTypeUtil;
import pro.jk.ejoker.common.utils.SerializableCheckerUtil;
import pro.jk.ejoker.common.utils.genericity.GenericDefinedType;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

/**
 * 对象关系二维化工具类
 * 
 * @author kimffy
 *
 * @param <ContainerKVP>
 * @param <ContainerVP>
 */
public class RelationshipTreeUtil<ContainerKVP, ContainerVP> extends AbstractRelationshipUtil<ContainerKVP, ContainerVP> {

	private final static Logger logger = LoggerFactory.getLogger(RelationshipTreeUtil.class);

	public RelationshipTreeUtil(IRelationshipScalpel<ContainerKVP, ContainerVP> eval,
			SpecialTypeCodecStore<?> specialTypeCodecStore) {
		super(eval, specialTypeCodecStore);
		Ensure.notNull(eval, "RelationshipTreeUtil.eval");
	}
	public RelationshipTreeUtil(IRelationshipScalpel<ContainerKVP, ContainerVP> eval) {
		this(eval, null);
	}

	public <T> Object getTreeStructure(Object target, TypeRefer<T> typeRef) {

		Type type = typeRef.getType();
		if(type instanceof ParameterizedType) {
			Class<?> topRawType = (Class<?> )((ParameterizedType )type).getRawType();
			if(SerializableCheckerUtil.hasSublevel(topRawType)) {
				GenericExpression ge = GenericExpressionFactory.getGenericExpress(ObjRef.class, type);
				ContainerKVP wrapKVP = getTreeStructure(ObjRef.of(target), ge);
				return eval.getFromKeyValeSet(wrapKVP, "target");
			}
		}
		
		return getTreeStructure(target, GenericExpressionFactory.getGenericExpress(type));
	}
	
	public ContainerKVP getTreeStructure(Object target) {
		Class<?> targetClazz = target.getClass();
		if(SerializableCheckerUtil.hasSublevel(targetClazz)) {
			throw new RuntimeException("Unsupport getTreeStructure() action on java collection util!!!");
		}
		
		if(GenericTypeUtil.ensureClassIsGenericType(targetClazz)) {
			throw new RuntimeException("Unsupport getTreeStructure() while top node genericity type");
		};
		
		GenericExpression targetExpression = GenericExpressionFactory.getGenericExpress(targetClazz);
		return getTreeStructure(target, targetExpression);
	}

	public ContainerKVP getTreeStructure(Object target, GenericExpression targetExpression) {
		Queue<IVoidFunction> queue = new ConcurrentLinkedQueue<>();
		
		String key = target.getClass().getSimpleName();
		ContainerKVP createNode = eval.createKeyValueSet();
		targetExpression.forEachFieldExpressionsDeeply(
				(fieldName, genericDefinedField) -> join(() -> {
					
					if(checkIgnoreField(genericDefinedField.field))
						return;
					
					Object fieldValue;
					try {
						fieldValue = genericDefinedField.field.get(target);
					} catch (IllegalArgumentException|IllegalAccessException e) {
						logger.error("Cannot access field!!! [target: {}]", key + "." + fieldName, e);
						throw new RuntimeException(e);
					}
					assemblyStructure(
							genericDefinedField.genericDefinedType,
							fieldValue,
							(result) -> eval.addToKeyValueSet(createNode, result, fieldName),
							() -> key + "." + fieldName,
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
				throw new RuntimeException(fmt("Unmatch genericity signature!!! [target: {}]", key));
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
		} else if (targetDefinedTypeMeta.isArray) {
			// 数组类型
			ContainerVP createValueSet = eval.createValueSet();
			node = createValueSet;
			
			if(definedClazz.isPrimitive()) {
				join(()-> privateTypeForEach(target, definedClazz, createValueSet), subTaskQueue);
			} else {
				Object[] objArray = (Object[])target;
				for(int i=0; i<objArray.length; i++) {
					int index = i;
					join( () -> 
						assemblyStructure(
								targetDefinedTypeMeta.componentTypeMeta,
								objArray[index],
								(result) -> eval.addToValueSet(createValueSet, result),
								() -> key + "[" + index + "]",
								subTaskQueue
						),
						subTaskQueue
					);
				};
			}
			
		} else if (SerializableCheckerUtil.hasSublevel(definedClazz)) {
			// Java集合类型
			if (target instanceof Queue) {
				if(!target.getClass().getSimpleName().endsWith("List"))
					throw new RuntimeException(fmt("Unsupport convert type java.util.Queue!!! [target: {}]", key));
			}
			if (target instanceof Collection) {
				ContainerVP createValueSet = eval.createValueSet();
				node = createValueSet;
				IVoidFunction1<Object> handle = item ->
					join( () ->
						assemblyStructure(
								targetDefinedTypeMeta.deliveryTypeMetasTable[0],
								item,
								(result) -> eval.addToValueSet(createValueSet, result),
								() -> key + "(#foreach in list)",
								subTaskQueue
						),
						subTaskQueue
					);
				if(target instanceof List)
					EachUtilx.forEach((List )target, handle);
				else
					EachUtilx.forEach((Set )target, handle);
			} else if (target instanceof Map) {
				GenericDefinedType pass1TypeMeta = targetDefinedTypeMeta.deliveryTypeMetasTable[0];
				if(!SerializableCheckerUtil.isDirectSerializableType(pass1TypeMeta.rawClazz))
					throw new RuntimeException(fmt(
							"We just support java base data type on the key while opera in serializing map!!! [target: {}, map.keyType: {}]",
							key,
							pass1TypeMeta.rawClazz.getSimpleName()));
				
				GenericDefinedType pass2TypeMeta = targetDefinedTypeMeta.deliveryTypeMetasTable[1];
				ContainerKVP createNode = eval.createKeyValueSet();
				node = createNode;
				EachUtilx.forEach((Map )target, (k, v) -> {
						join(() -> assemblyStructure(
								pass2TypeMeta,
								v,
								(result) -> eval.addToKeyValueSet(createNode, result, k.toString()),
								() -> key + "[" + k.toString() + "]",
								subTaskQueue),
							subTaskQueue);
					}
				);
			}
		} else {
			{
				/// 不支持部分数据类型。
				if (UnsupportTypes.isUnsupportType(valueClazz)) {
					throw new RuntimeException(fmt("Unsupport type!!![target: {}, valuePrototype: {}, actually: {}]",
							key,
							targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
							valueClazz.getName()));
				}
				if (UnsupportTypes.isUnsupportType(definedClazz)) {
					throw new RuntimeException(fmt("Unsupport type!!![target: {}, prototype: {}, actually: {}]",
							key,
							targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
							definedClazz.getName()));
				}
			}
			/// 普通对象类型
			ContainerKVP createNode = eval.createKeyValueSet();
			node = createNode;
			GenericExpression genericExpress = GenericExpressionFactory.getGenericExpressDirectly(definedClazz,
					targetDefinedTypeMeta.deliveryTypeMetasTable);
			genericExpress.forEachFieldExpressionsDeeply(
					(fieldName, genericDefinedField) -> join(() -> {
						
						if(checkIgnoreField(genericDefinedField.field))
							return;
						
						Object fieldValue;
						try {
							fieldValue = genericDefinedField.field.get(target);
						} catch (IllegalArgumentException|IllegalAccessException e) {
							logger.error("Cannot access field!!! [target: {}]", key, e);
							throw new RuntimeException(e);
						}
						assemblyStructure(
								genericDefinedField.genericDefinedType,
								fieldValue,
								(result) -> eval.addToKeyValueSet(createNode, result, fieldName),
								() -> key + "." + fieldName,
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
	
}
