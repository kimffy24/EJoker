package com.jiefzz.ejoker.z.common.utils.relationship;

import static com.jiefzz.ejoker.z.common.utils.relationship.RelationshipTreeUtil.checkIgnoreField;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.utils.Ensure;
import com.jiefzz.ejoker.z.common.utils.InstanceBuilder;
import com.jiefzz.ejoker.z.common.utils.ParameterizedTypeUtil;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericDefinedTypeMeta;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpression;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpressionFactory;

/**
 * 对象关系二维化工具类
 * 
 * @author kimffy
 *
 * @param <ContainerKVP>
 * @param <ContainerVP>
 */
public class RelationshipTreeRevertUtil<ContainerKVP, ContainerVP> extends AbstractRelationshipUtil{

	private final static Logger logger = LoggerFactory.getLogger(RelationshipTreeRevertUtil.class);

	private final IRelationshipTreeDisassemblers<ContainerKVP, ContainerVP> disassemblyEval;
	
	public RelationshipTreeRevertUtil(IRelationshipTreeDisassemblers<ContainerKVP, ContainerVP> disassemblyEval,
			SpecialTypeCodecStore<?> specialTypeCodecStore) {
		super(specialTypeCodecStore);
		this.disassemblyEval = disassemblyEval;
		Ensure.notNull(disassemblyEval, "RelationshipTreeRevertUtil.disassemblyEval");
	}
	
	public RelationshipTreeRevertUtil(IRelationshipTreeDisassemblers<ContainerKVP, ContainerVP> disassemblyEval) {
		this(disassemblyEval, null);
	}
	
	public <T> T revert(ContainerKVP kvDataSet, Class<T> clazz) {
		
		T revertValue = (T )revertInternal(kvDataSet, GenericExpressionFactory.getGenericExpress(clazz));

		IVoidFunction task;
		while(null != (task = taskQueueBox.get().poll())) {
			task.trigger();
		};
		
		return revertValue;
		
	}
	
	private Object revertInternal(ContainerKVP kvDataSet, GenericExpression expression) { 
		Object instance = (new InstanceBuilder(expression.getDeclarePrototype())).doCreate();
		expression.forEachFieldExpressionsDeeply(
				(fieldName, genericDefinedField) -> { 
						if(
								checkIgnoreField(genericDefinedField.field)
								|| !disassemblyEval.hasKey(kvDataSet, fieldName)) {
							return;
						}
						disassemblyStructure(
							genericDefinedField.genericDefinedTypeMeta,
							disassemblyEval.getValue(kvDataSet, fieldName),
							result -> setField(genericDefinedField.field, instance, result)
						);
				}
		);
		return instance;
	}
	
	private void disassemblyStructure(GenericDefinedTypeMeta targetDefinedTypeMeta, Object serializedValue, IVoidFunction1<Object> effector) {
		
		if(null == serializedValue) {
			if(!targetDefinedTypeMeta.rawClazz.isPrimitive())
				effector.trigger(null);
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
				int size = disassemblyEval.getVPSize((ContainerVP )serializedValue);
				Object[] newArray = (Object[] )Array.newInstance(definedClazz, size);
				revertedResult = newArray;
				for(int i=0; i<size; i++) {
					final int idx = i;
					join(
							() -> disassemblyStructure(
									targetDefinedTypeMeta.componentTypeMeta,
									disassemblyEval.getValue((ContainerVP )serializedValue, idx),
									result -> newArray[idx] = result
									)
					);
				}
			}
		} else if(null != (specialTypeCodec = getDeserializeCodec(definedClazz))) {
			revertedResult = specialTypeCodec.decode(serializedValue);
		} else if(ParameterizedTypeUtil.isDirectSerializableType(definedClazz)) {
			/// 定义为可直接序列化类型
			revertedResult = serializedValue;
		} else if (definedClazz.isEnum()) {
			// 枚举还原
			if(!String.class.equals(serializedValue)) {
				revertedResult = revertIntoEnumType(definedClazz, (String )serializedValue);
			} else {
				logger.warn("Enum data should represent as a String!");
				throw new RuntimeException(String.format("Revert %s#%s faild!!! serializedValue: %s", "", "", serializedValue.toString()));
			}
		} else if (ParameterizedTypeUtil.hasSublevel(definedClazz)) {
			if (Queue.class.isAssignableFrom(definedClazz)) {
				throw new RuntimeException("Unsupport revert type java.util.Queue!!!");
			} else if (Collection.class.isAssignableFrom(definedClazz)) {
				if(List.class.isAssignableFrom(definedClazz)) {
					revertedResult = new ArrayList();
				} else {
					revertedResult = new HashSet();
				}
				int size = disassemblyEval.getVPSize((ContainerVP )serializedValue);
				for(int i=0; i<size; i++) {
					final int idx = i;
					join(
							() -> disassemblyStructure(
									targetDefinedTypeMeta.deliveryTypeMetasTable[0],
									disassemblyEval.getValue((ContainerVP )serializedValue, idx),
									result -> ((Collection )revertedResult).add(result)
								)
					);
				}
			} else {
				/// map的情况
				/// 按照RelationshipTreeUtil的转化 map的key一定是string类型的。
				revertedResult = new HashMap();
				GenericDefinedTypeMeta valueTypeMeta = targetDefinedTypeMeta.deliveryTypeMetasTable[1];
				Set keySet = disassemblyEval.getKeySet((ContainerKVP )serializedValue);
				for(Object key:keySet) {
					join(
							() -> disassemblyStructure(
									valueTypeMeta,
									disassemblyEval.getValue((ContainerKVP )serializedValue, (String )key),
									result -> ((Map )revertedResult).put((String )key, result)
								)
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
			revertedResult = revertInternal((ContainerKVP )serializedValue, GenericExpressionFactory.getGenericExpress(definedClazz, targetDefinedTypeMeta.deliveryTypeMetasTable));
		}
		effector.trigger(revertedResult);
	}
	
	private void join(IVoidFunction task) {
		if(!taskQueueBox.get().offer(task)) {
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
			int size = disassemblyEval.getVPSize(vpNode);
			int[] rArray = (int[] )Array.newInstance(int.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (int )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (long.class == componentType) {
			// long
			int size = disassemblyEval.getVPSize(vpNode);
			long[] rArray = (long[] )Array.newInstance(long.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (long )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (short.class == componentType) {
			// short
			int size = disassemblyEval.getVPSize(vpNode);
			short[] rArray = (short[] )Array.newInstance(short.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (short )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (double.class == componentType) {
			// double
			int size = disassemblyEval.getVPSize(vpNode);
			double[] rArray = (double[] )Array.newInstance(double.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (double )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (float.class == componentType) {
			// float
			int size = disassemblyEval.getVPSize(vpNode);
			float[] rArray = (float[] )Array.newInstance(float.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (float )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (char.class == componentType) {
			// char
			int size = disassemblyEval.getVPSize(vpNode);
			char[] rArray = (char[] )Array.newInstance(char.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (char )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (byte.class == componentType) {
			// char
			int size = disassemblyEval.getVPSize(vpNode);
			byte[] rArray = (byte[] )Array.newInstance(byte.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (byte )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else if (boolean.class == componentType) {
			// boolean
			int size = disassemblyEval.getVPSize(vpNode);
			boolean[] rArray = (boolean[] )Array.newInstance(boolean.class, size);
			for(int i=0; i<size; i++)
				rArray[i] = (boolean )disassemblyEval.getValue(vpNode, i);
			result = rArray;
		} else {
			// this should never happen!!!
			throw new RuntimeException();
		}
		return result;
	}
	
	
}
