package com.jiefzz.ejoker.z.common.utilities.relationship;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.Set;

import org.omg.CORBA.PUBLIC_MEMBER;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utilities.relationship.SpecialTypeCodec;

/**
 * 对象关系还原类
 * @author kimffy
 *
 * @param <ContainerKVP> 维度化时使用的键值集类型
 * @param <ContainerVP>维度化时使用的值集类型
 */
public class RevertRelationshipTreeUitl<ContainerKVP, ContainerVP> extends AbstractTypeAnalyze {

	private final static Logger logger = LoggerFactory.getLogger(RevertRelationshipTreeUitl.class);

	private final RevertRelationshipTreeDisassemblyInterface<ContainerKVP, ContainerVP> disassemblyWorker;
	
	private SpecialTypeCodecStore<?> specialTypeCodecStore = null;
	
	public RevertRelationshipTreeUitl(RevertRelationshipTreeDisassemblyInterface<ContainerKVP, ContainerVP> disassemblyWorker){
		this.disassemblyWorker = disassemblyWorker;
	}
	public RevertRelationshipTreeUitl(RevertRelationshipTreeDisassemblyInterface<ContainerKVP, ContainerVP> disassemblyWorker, SpecialTypeCodecStore<?> specialTypeHandler) {
		this(disassemblyWorker);
		this.specialTypeCodecStore = specialTypeHandler;
	}

	public <T> T revert(Class<T> clazz, ContainerKVP source) {
		return internalRevert(new SerializingContext().process(clazz.getName()), clazz, source);
	}

	public <T> T internalRevert(SerializingContext context, Class<T> clazz, ContainerKVP source) {

		T newInstance=null;

		try {
			newInstance = clazz.newInstance();
		} catch (Exception e) {
			String format = String.format("Could not revert [%s]", clazz.getName());
			logger.error(format);
			throw new RuntimeException(format, e);
		}
		
		Object value;
		Class<?> valueType;
		
		Map<String, Field> analyzeClazzInfo = analyzeClazzInfo(clazz);
		Set<Entry<String,Field>> entrySet = analyzeClazzInfo.entrySet();
		for(Entry<String,Field> entry : entrySet){
			String fieldName = entry.getKey();
			if(null == (value = disassemblyWorker.getValue(source, fieldName)))
				continue;
			valueType = value.getClass();
			Field field = entry.getValue();
			Class<?> fieldType = field.getType();
			field.setAccessible(true);
			context.process(fieldName);
			try {
				Object specialValue;
				if (null != (specialValue = processWithUserSpecialCodec(value, valueType, fieldType))) {
					field.set(newInstance, specialValue);
				} else if(ParameterizedTypeUtil.isDirectSerializableType(fieldType)) {
					// 基础数据还原
					field.set(newInstance, directSerializableTypeRevert(ParameterizedTypeUtil.getPrimitiveObjectType(fieldType), value));
				} else if (ParameterizedTypeUtil.hasSublevel(fieldType)) {
					// Java集合类型
					if(Queue.class.isAssignableFrom(fieldType)) {
						throw new RuntimeException("Unsupport revert type java.util.Queue!!!");
					}
					if(Map.class.isAssignableFrom(fieldType)) {
						field.set(newInstance, disassemblyWorker.convertNodeAsMap(disassemblyWorker.getChildKVP(source, fieldName)));
					} else if(Set.class.isAssignableFrom(fieldType)) {
						field.set(newInstance, disassemblyWorker.convertNodeAsSet(disassemblyWorker.getChildVP(source, fieldName)));
					} else if(List.class.isAssignableFrom(fieldType)) {
						field.set(newInstance, disassemblyWorker.convertNodeAsList(disassemblyWorker.getChildVP(source, fieldName)));
					}
				} else if(fieldType.isEnum()) {
					// 枚举还原
					if(String.class.equals(valueType)) {
						field.set(newInstance, revertIntoEnumType(fieldType, (String )value));
					} else {
						logger.warn("Enum data should represent as a String!");
						throw new RuntimeException(String.format("Revert %s#%s faild!!! target: %s", clazz, fieldName, context.getCoordinate()));
					}
				} else if(fieldType.isArray()) {
					// 数组
					ContainerVP vpNode = disassemblyWorker.getChildVP(source, fieldName);
					Class<?> componentType = fieldType.getComponentType();
					if(!componentType.isPrimitive())
						field.set(newInstance, revertIntoArray(context, componentType, vpNode));
					// TODO  java 没有原生的委托，如果有就直接委托了。。。自定义的委托模式并没有8连if的效率。。。哎。。。下同
					else if(componentType==int.class)
						field.set(newInstance, revertIntoArrayInt(vpNode));
					else if(componentType==long.class)
						field.set(newInstance, revertIntoArrayLong(vpNode));
					else if(componentType==short.class)
						field.set(newInstance, revertIntoArrayShort(vpNode));
					else if(componentType==double.class)
						field.set(newInstance, revertIntoArrayDouble(vpNode));
					else if(componentType==float.class)
						field.set(newInstance, revertIntoArrayFloat(vpNode));
					else if(componentType==char.class)
						field.set(newInstance, revertIntoArrayCharacter(vpNode));
					else if(componentType==byte.class)
						field.set(newInstance, revertIntoArrayByte(vpNode));
					else if(componentType==boolean.class)
						field.set(newInstance, revertIntoArrayBoolean(vpNode));
				} else {
					// 常规对象
					SpecialTypeCodec handler;
					if(null != specialTypeCodecStore && null != (handler = specialTypeCodecStore.getCodec(fieldType))) {
						// 如果有存在 用户指定的解析器
						field.set(newInstance, handler.decode(value));
					} else if(fieldType==Object.class && ParameterizedTypeUtil.isDirectSerializableType(valueType)) {
						// 可以接受的泛型。
						field.set(newInstance, value);
					} else {

						// 不支持部分数据类型。
						if(UnsupportTypes.isUnsupportType(fieldType))
							throw new RuntimeException(
									String.format("Unsupport field type %s!!! target: %s", fieldType.getName(), context.getCoordinate())
							);
						if(UnsupportTypes.isUnsupportType(valueType))
							throw new RuntimeException(
									String.format("Unsupport value type %s!!! target: %s", valueType.getName(), context.getCoordinate())
							);
						field.set(newInstance, internalRevert(context, fieldType, disassemblyWorker.getChildKVP(source, fieldName)));
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(String.format("Revert faild on [%s]'s field [%s]!!! target: %s", clazz.getName(), fieldName, context.getCoordinate()), e);
			}
			context.shot();
		}

		return newInstance;
	}

//	/**
//	 * 还原枚举类
//	 * @param enumType 枚举类型
//	 * @param value 源值
//	 * @param targetClazz 所在对象类型（用于错误显示）
//	 * @param fieldName 所在对象的属性名（用于错误显示）
//	 * @return 目标值
//	 */
//	private <TEnum> TEnum revertIntoEnum(Class<TEnum> enumType, Object value, Class<?> targetClazz, String fieldName){
//		if(value.getClass()==int.class || value.getClass()==Integer.class)
//			return revertIntoEnumType(enumType, (Integer )value);
//		else if(value.getClass()==String.class)
//			return revertIntoEnumType(enumType, (String )value);
//		else
//			throw new RuntimeException(String.format("Revert %s#%s faild!!!", targetClazz, fieldName));
//	}
//	
//	/**
//	 * 还原枚举类型，通过枚举Index
//	 */
//	private <TEnum> TEnum revertIntoEnumType(Class<TEnum> enumType, int index){
//		if(enumType.isEnum())
//			return enumType.getEnumConstants()[index];
//		throw new RuntimeException(String.format("[%s] is not a Enum type!!!", enumType.getName()));
//	}

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
	
	private Map<Class<Enum<?>>, Map<String, Enum<?>>> eMap = new ConcurrentHashMap<>();
	private final static Map<String, Enum<?>> eMapItemPlaceHolder = new HashMap<>();

	/**
	 * 在回显对象过程中，会可能出现类型匹配不上的情况。
	 * @param fieldType
	 * @param rawValue
	 * @return
	 */
	private Object directSerializableTypeRevert(Class<?> fieldType, Object rawValue){
		if(fieldType==Long.class || fieldType==long.class)
			return ((Number )rawValue).longValue();
		else if(fieldType==Integer.class || fieldType==int.class)
			return ((Number )rawValue).intValue();
		else if(fieldType==Double.class || fieldType==double.class)
			return ((Number )rawValue).doubleValue();
		else if(fieldType==Float.class || fieldType==float.class)
			return ((Number )rawValue).floatValue();
		else if(fieldType==Short.class || fieldType==short.class)
			return ((Number )rawValue).shortValue();
		return rawValue;
	}


	/**
	 * 回显数组。
	 * TODO: 未测试！！！
	 * @param arrayType
	 * @param valueArray
	 * @return
	 */
	public <TComponent> TComponent[] revertIntoArray(SerializingContext context, Class<TComponent> componentType, ContainerVP vpNode) {
		int size = disassemblyWorker.getVPSize(vpNode);
		TComponent[] rArray = (TComponent[] )Array.newInstance(componentType, size);
		SpecialTypeCodec handler;
		Object specialValue;
		if(null != specialTypeCodecStore && null != (handler = specialTypeCodecStore.getCodec(componentType))) {
			for(int i=0; i<size; i++) {
				context.process(i);
				rArray[i] = (TComponent )handler.decode(disassemblyWorker.getValue(vpNode, i));
				context.shot();
			}
		} else if(ParameterizedTypeUtil.isDirectSerializableType(componentType)) {
			for(int i=0; i<size; i++)
				rArray[i] = (TComponent )directSerializableTypeRevert(componentType, disassemblyWorker.getValue(vpNode, i));
		} else if(ParameterizedTypeUtil.hasSublevel(componentType)) {
			throw new RuntimeException("Unsupport revert Java Collection/Map in Java Array!!!");
		} else if(componentType.isEnum()) {
			for(int i=0; i<size; i++) {
				Object rawValue = disassemblyWorker.getValue(vpNode, i);
				Class<?> rawValueType = rawValue.getClass();
				if(String.class.equals(rawValueType)) {
					rArray[i] = (TComponent )revertIntoEnumType(componentType, (String )rawValue);
				} else {
					logger.warn("Enum data should represent as a String!");
					throw new RuntimeException(String.format("Revert %s#%s faild!!!", "lost", "[]lost"));
				}
			}
		} else if(componentType.isArray()) {
			Class<?> childComponentType = componentType.getComponentType();
			if(!childComponentType.isPrimitive())
				for(int i=0; i<size; i++) {
					context.process(i);
					rArray[i] = (TComponent )revertIntoArray(context, childComponentType, disassemblyWorker.getChildVP(vpNode, i));
					context.shot();
				}
			else if(childComponentType==int.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayInt(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==long.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayLong(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==short.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayShort(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==double.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayDouble(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==float.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayFloat(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==char.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayCharacter(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==byte.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayByte(disassemblyWorker.getChildVP(vpNode, i));
			else if(childComponentType==boolean.class)
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArrayBoolean(disassemblyWorker.getChildVP(vpNode, i));
		} else {
			for(int i=0; i<size; i++) {
				context.process(i);
				Object value = disassemblyWorker.getValue(vpNode, i);
				if (componentType==Object.class && ParameterizedTypeUtil.isDirectSerializableType(value)) {
//					 可以接受的泛型。
					rArray[i] = (TComponent )directSerializableTypeRevert(Object.class, value);
				} else {
					Class<?> valueType = value.getClass();
					// 不支持部分数据类型。
					if(UnsupportTypes.isUnsupportType(componentType))
						throw new RuntimeException(
								String.format("Unsupport component type %s!!! target: %s", componentType.getName(), context.getCoordinate())
						);
					if(UnsupportTypes.isUnsupportType(valueType))
							throw new RuntimeException(
									String.format("Unsupport value type %s!!! target: %s", valueType, context.getCoordinate())
							);
					if(componentType==Object.class) {
						// 使用泛型时，类型丢失！！！ 如果构造object类型对象装载数据，将会丢失全部数据！！！！
						String info = String.format("We lost object type info here!!! target: %s", context.getCoordinate());
						logger.error(info);
						if(true)
							throw new RuntimeException(info);
						else 
							continue;
					}
					rArray[i] = internalRevert(context, componentType, disassemblyWorker.getChildKVP(vpNode, i));
				}
				context.shot();
					
			}
		}
		return rArray;
	}
	
	/**
	 * int array
	 * @param vpNode
	 * @return
	 */
	public int[] revertIntoArrayInt(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		int[] rArray = (int[] )Array.newInstance(int.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Integer )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}
	
	/**
	 * long array
	 * @param vpNode
	 * @return
	 */
	public long[] revertIntoArrayLong(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		long[] rArray = (long[] )Array.newInstance(long.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Long )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}
	
	/**
	 * short array
	 * @param vpNode
	 * @return
	 */
	public short[] revertIntoArrayShort(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		short[] rArray = (short[] )Array.newInstance(short.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Short )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}
	
	/**
	 * double array
	 * @param vpNode
	 * @return
	 */
	public double[] revertIntoArrayDouble(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		double[] rArray = (double[] )Array.newInstance(double.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Double )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}
	
	/**
	 * float array
	 * @param vpNode
	 * @return
	 */
	public float[] revertIntoArrayFloat(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		float[] rArray = (float[] )Array.newInstance(float.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Float )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}
	
	/**
	 * char array
	 * @param vpNode
	 * @return
	 */
	public char[] revertIntoArrayCharacter(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		char[] rArray = (char[] )Array.newInstance(char.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Character )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}
	
	/**
	 * byte array
	 * @param vpNode
	 * @return
	 */
	public byte[] revertIntoArrayByte(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		byte[] rArray = (byte[] )Array.newInstance(byte.class, size);
		for(int i=0; i<size; i++) {
			Number value = (Number )disassemblyWorker.getValue(vpNode, i);
			rArray[i] = value.byteValue();
		}
		return rArray;
	}
	
	/**
	 * boolean array
	 * @param vpNode
	 * @return
	 */
	public boolean[] revertIntoArrayBoolean(ContainerVP vpNode){
		int size = disassemblyWorker.getVPSize(vpNode);
		boolean[] rArray = (boolean[] )Array.newInstance(boolean.class, size);
		for(int i=0; i<size; i++)
			rArray[i] = (Boolean )disassemblyWorker.getValue(vpNode, i);
		return rArray;
	}

	
	private Object processWithUserSpecialCodec(Object value, Class<?> valueType, Class<?> fieldType) {
		if(null == specialTypeCodecStore)
			return null;
		
		SpecialTypeCodec codec = specialTypeCodecStore.getCodec(fieldType);
		if(null == codec)
			codec.decode(value);
		
		return null;
	}
}
