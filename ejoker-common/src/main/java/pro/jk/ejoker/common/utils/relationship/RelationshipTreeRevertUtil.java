package pro.jk.ejoker.common.utils.relationship;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
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
public class RelationshipTreeRevertUtil<ContainerKVP, ContainerVP> extends AbstractRelationshipUtil<ContainerKVP, ContainerVP>{

	private final static Logger logger = LoggerFactory.getLogger(RelationshipTreeRevertUtil.class);
	
	public RelationshipTreeRevertUtil(IRelationshipScalpel<ContainerKVP, ContainerVP> disassemblyEval,
			SpecialTypeCodecStore<?> specialTypeCodecStore) {
		super(disassemblyEval, specialTypeCodecStore);
	}
	
	public RelationshipTreeRevertUtil(IRelationshipScalpel<ContainerKVP, ContainerVP> disassemblyEval) {
		this(disassemblyEval, null);
	}

	public <T> T revert(Object kvDataSet, TypeRefer<T> typeRef) {

		Type type = typeRef.getType();
		if(type instanceof ParameterizedType) {
			Class<?> topRawType = (Class<?> )((ParameterizedType )type).getRawType();
			if(SerializableCheckerUtil.hasSublevel(topRawType)) {
				GenericExpression ge = GenericExpressionFactory.getGenericExpress(ObjRef.class, type);
				ContainerKVP wrap = eval.createKeyValueSet();
				eval.addToKeyValueSet(wrap, kvDataSet, "target");
				ObjRef<T> res = (ObjRef<T> )revert(wrap, ge);
				return res.getTarget();
			}
		}
		
		return revert((ContainerKVP )kvDataSet, GenericExpressionFactory.getGenericExpress(typeRef.getType()));
	}
	
	public <T> T revert(ContainerKVP kvDataSet, Class<T> clazz) {
		return revert(kvDataSet, GenericExpressionFactory.getGenericExpress(clazz));
	}

	
	public <T> T revert(ContainerKVP kvDataSet, GenericExpression genericExpress) {

		Queue<IVoidFunction> queue = new ConcurrentLinkedQueue<>();
		T revertValue = (T )revertInternal(kvDataSet, genericExpress, queue);

		IVoidFunction task;
		while(null != (task = queue.poll())) {
			task.trigger();
		};
		
		return revertValue;
		
	}
	
	private Object revertInternal(ContainerKVP kvDataSet, GenericExpression expression, Queue<IVoidFunction> subTaskQueue) { 
		Object instance = doCreateInstance(expression.getDeclarePrototype());
		expression.forEachFieldExpressionsDeeply(
				(fieldName, genericDefinedField) -> { 
						if(
								checkIgnoreField(genericDefinedField.field)
								|| !eval.hasKey(kvDataSet, fieldName)) {
							return;
						}
						disassemblyStructure(
							genericDefinedField.genericDefinedType,
							eval.getFromKeyValeSet(kvDataSet, fieldName),
							result -> setField(genericDefinedField.field, instance, result.trigger()),
							subTaskQueue
						);
				}
		);
		return instance;
	}
	
	private void disassemblyStructure(GenericDefinedType targetDefinedTypeMeta, Object serializedValue, IVoidFunction1<IFunction<Object>> effector, Queue<IVoidFunction> subTaskQueue) {
		
		if(null == serializedValue) {
			if(!targetDefinedTypeMeta.rawClazz.isPrimitive())
				effector.trigger(() -> null);
			return;
		}
		
		if(!targetDefinedTypeMeta.allHasMaterialized)
			throw new RuntimeException("Unmatch genericity signature!!!");
		
		final Class<?> definedClazz = targetDefinedTypeMeta.rawClazz;
		
		Object revertedResult;
		SpecialTypeCodec specialTypeCodec;
		if(targetDefinedTypeMeta.isArray) {
			if(!targetDefinedTypeMeta.hasGenericDeclare && definedClazz.isPrimitive()) {
				revertedResult = revertPrivateTypeArray((ContainerVP )serializedValue, definedClazz);
			} else {
				// common
				int size = eval.getVPSize((ContainerVP )serializedValue);
				Object[] newArray = (Object[] )Array.newInstance(definedClazz, size);
				revertedResult = newArray;
				for(int i=0; i<size; i++) {
					final int idx = i;
					join(
							() -> disassemblyStructure(
									targetDefinedTypeMeta.componentTypeMeta,
									eval.getValue((ContainerVP )serializedValue, idx),
									result -> newArray[idx] = result.trigger(),
									subTaskQueue
									),
							subTaskQueue
					);
				}
			}
		} else if(null != (specialTypeCodec = getDeserializeCodec(definedClazz))) {
			revertedResult = specialTypeCodec.decode(serializedValue);
		} else if(SerializableCheckerUtil.isDirectSerializableType(definedClazz)) {
			/// 定义为可直接序列化类型
			revertedResult = baseTypeConvert(definedClazz, serializedValue);
		} else if (definedClazz.isEnum()) {
			// 枚举还原
			if(!String.class.equals(serializedValue)) {
				revertedResult = revertIntoEnumType(definedClazz, (String )serializedValue);
			} else {
				logger.warn("Enum data should represent as a String!");
				throw new RuntimeException(String.format("Revert %s#%s faild!!! serializedValue: %s", "", "", serializedValue.toString()));
			}
		} else if (SerializableCheckerUtil.hasSublevel(definedClazz)) {
			if (Queue.class.isAssignableFrom(definedClazz)) {
				if(!definedClazz.getSimpleName().endsWith("List"))
					throw new RuntimeException("Unsupport revert type java.util.Queue!!!");
			}
			if (Collection.class.isAssignableFrom(definedClazz)) {
				ContainerVP vp;
				if(List.class.isAssignableFrom(definedClazz)) {
					vp = (ContainerVP )serializedValue;
					revertedResult = new LinkedList();
				} else if (serializedValue instanceof Set){
					vp = ((ContainerVP )new ArrayList<>((Set<?> )serializedValue));
					revertedResult = new HashSet();
				} else {
					throw new RuntimeException(String.format("Unsupport container type!!! [type: {}]", serializedValue.getClass().getName()));
				}
				int size = eval.getVPSize(vp);
				for(int i=0; i<size; i++) {
					final int idx = i;
					join(
							() -> disassemblyStructure(
									targetDefinedTypeMeta.deliveryTypeMetasTable[0],
									eval.getValue(vp, idx),
									result -> ((Collection )revertedResult).add(result.trigger()),
									subTaskQueue
								),
							subTaskQueue
					);
				}
			} else {
				/// map的情况
				/// 按照RelationshipTreeUtil的转化 map的key一定是string类型的。
				revertedResult = new HashMap();
				GenericDefinedType valueTypeMeta = targetDefinedTypeMeta.deliveryTypeMetasTable[1];
				Set keySet = eval.getKeySet((ContainerKVP )serializedValue);
				for(Object key:keySet) {
					join(
							() -> disassemblyStructure(
									valueTypeMeta,
									eval.getFromKeyValeSet((ContainerKVP )serializedValue, (String )key),
									result -> ((Map )revertedResult).put((String )key, result.trigger()),
									subTaskQueue
								),
							subTaskQueue
					);
				}
			}
		} else {
			{
				/// 不支持部分数据类型。
				if (UnsupportTypes.isUnsupportType(definedClazz)) {
					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on %s#%s", definedClazz.getName(),
							targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
							"(# lose in foreach)"));
				}
				if (UnsupportTypes.isUnsupportType(serializedValue.getClass())) {
					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on %s#%s", definedClazz.getName(),
							targetDefinedTypeMeta.getGenericDefination().genericPrototypeClazz.getName(),
							"(# lose in foreach)"));
				}
			}
			revertedResult = revertInternal((ContainerKVP )serializedValue, GenericExpressionFactory.getGenericExpressDirectly(definedClazz, targetDefinedTypeMeta.deliveryTypeMetasTable), subTaskQueue);
		}
		effector.trigger(() -> revertedResult);
	}
	
	private void join(IVoidFunction task, Queue<IVoidFunction> subTaskQueue) {
		if(!subTaskQueue.offer(task)) {
			throw new RuntimeException("Task Queue has no more capacity!!!");
		}
	}
	
	private void setField(Field field, Object instance, Object value) {
		try {
			field.set(instance, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Cannot access field!!!", e);
			throw new RuntimeException(e);
		}
	}
	
	private Object baseTypeConvert(Class<?> definedClazz, Object target) {
		Object res = null;
		Class<?> targetClazz = target.getClass();
		if(definedClazz.equals(targetClazz)) {
			res = target;
		} else if(Number.class.isAssignableFrom(targetClazz)) {
			if(Long.class.equals(definedClazz) || long.class.equals(definedClazz)) {
				res = ((Number )target).longValue();
			} else if(Integer.class.equals(definedClazz) || int.class.equals(definedClazz)) {
				res = ((Number )target).intValue();
			} else if(Short.class.equals(definedClazz) || short.class.equals(definedClazz)) {
				res = ((Number )target).shortValue();
			} else if(Byte.class.equals(definedClazz) || byte.class.equals(definedClazz)) {
				res = ((Number )target).byteValue();
			} else if(Double.class.equals(definedClazz) || double.class.equals(definedClazz)) {
				res = ((Number )target).doubleValue();
			} else if(Float.class.equals(definedClazz) || float.class.equals(definedClazz)) {
				res = ((Number )target).floatValue();
			}
		} else if(
				(Character.class.equals(definedClazz) || char.class.equals(definedClazz)) &&
				(Character.class.equals(targetClazz) || char.class.equals(targetClazz))
			) {
			res = target;
		} else if(
				(Boolean.class.equals(definedClazz) || boolean.class.equals(definedClazz)) &&
				(Boolean.class.equals(targetClazz) || boolean.class.equals(targetClazz))
			) {
			res = target;
		}

		if(null == res)
			throw new RuntimeException(String.format("Type convert faild!!! defined type: [%s], data type: [%s], data value: ", definedClazz.getName(), target.getClass().getName()) + target);
		return res;
	}

	/**
	 * 还原枚举类型，通过枚举的表现字符值
	 */
	private <TEnum> TEnum revertIntoEnumType(Class<TEnum> enumType, String represent){
		Object value = null;
		if(enumType.isEnum()) {
			Map<String, Enum<?>> eInfoMap;
			if(eMapItemPlaceHolder.equals(eInfoMap = eMap.getOrDefault(enumType, eMapItemPlaceHolder))) {
				eInfoMap = new HashMap<>();
				TEnum[] enumConstants = enumType.getEnumConstants();
				for(TEnum obj:enumConstants) {
					eInfoMap.put(obj.toString(), (Enum<?> )obj);
				}
				eMap.putIfAbsent((Class<Enum<?>> )enumType, eInfoMap);
			};
			value = eInfoMap.get(represent);
		} else {
			throw new RuntimeException(String.format("[%s] is not a Enum type!!!", enumType.getName()));
		}
		if(null == value) {
			throw new RuntimeException(String.format("[%s] has not such a value[%s]!!!", enumType.getName(), represent));
		}
		return (TEnum )value;
	}
	private Map<Class<Enum<?>>, Map<String, Enum<?>>> eMap = new HashMap<>();
	private final static Map<String, Enum<?>> eMapItemPlaceHolder = new HashMap<>();
	
	/**
	 * 尽量减少装箱操作。<br>
	 * * 皆因java不支持基数类型的数组泛化
	 * @param valueSet
	 * @param componentType
	 * @param object
	 */
	private Object revertPrivateTypeArray(ContainerVP vpNode, Class<?> componentType) {
		Object result;
		if (int.class == componentType) {
			// integer
			int size = eval.getVPSize(vpNode);
			int[] rArray = (int[] )Array.newInstance(int.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (int )eval.getValue(vpNode, i);
			result = rArray;
		} else if (long.class == componentType) {
			// long
			int size = eval.getVPSize(vpNode);
			long[] rArray = (long[] )Array.newInstance(long.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (long )eval.getValue(vpNode, i);
			result = rArray;
		} else if (short.class == componentType) {
			// short
			int size = eval.getVPSize(vpNode);
			short[] rArray = (short[] )Array.newInstance(short.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (short )eval.getValue(vpNode, i);
			result = rArray;
		} else if (double.class == componentType) {
			// double
			int size = eval.getVPSize(vpNode);
			double[] rArray = (double[] )Array.newInstance(double.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (double )eval.getValue(vpNode, i);
			result = rArray;
		} else if (float.class == componentType) {
			// float
			int size = eval.getVPSize(vpNode);
			float[] rArray = (float[] )Array.newInstance(float.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (float )eval.getValue(vpNode, i);
			result = rArray;
		} else if (char.class == componentType) {
			// char
			int size = eval.getVPSize(vpNode);
			char[] rArray = (char[] )Array.newInstance(char.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (char )eval.getValue(vpNode, i);
			result = rArray;
		} else if (byte.class == componentType) {
			// char
			int size = eval.getVPSize(vpNode);
			byte[] rArray = (byte[] )Array.newInstance(byte.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (byte )eval.getValue(vpNode, i);
			result = rArray;
		} else if (boolean.class == componentType) {
			// boolean
			int size = eval.getVPSize(vpNode);
			boolean[] rArray = (boolean[] )Array.newInstance(boolean.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (boolean )eval.getValue(vpNode, i);
			result = rArray;
		} else {
			// this should never happen!!!
			throw new RuntimeException();
		}
		return result;
	}

	private <T> T doCreateInstance(Class<T> clazz) {
			Object newInstance;
			try {
				newInstance = clazz.newInstance();
			} catch (InstantiationException|IllegalAccessException e) {
				String errInfo = StringUtilx.fmt("Connot create new instance!!! [type: {}]", clazz.getName());
				logger.error(errInfo, e);
				throw new RuntimeException(errInfo, e);
			}
			return (T )newInstance;
	}
	
}
