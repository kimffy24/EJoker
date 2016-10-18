package com.jiefzz.ejoker.utils.relationship;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RevertRelationshipTreeUitl<ContainerKVP, ContainerVP> extends AbstractTypeAnalyze {

	private final static Logger logger = LoggerFactory.getLogger(RevertRelationshipTreeUitl.class);			// 日志

	private RevertRelationshipTreeDisassemblyInterface<ContainerKVP, ContainerVP> disassemblyWorker;

	public RevertRelationshipTreeUitl(RevertRelationshipTreeDisassemblyInterface<ContainerKVP, ContainerVP> disassemblyWorker){
		this.disassemblyWorker = disassemblyWorker;
	}

	public <T> T revert(Class<T> clazz, ContainerKVP source) {

		T newInstance=null;

		try {
			newInstance = clazz.newInstance();
		} catch (Exception e) {
			String format = String.format("Could not revert [%s]", clazz.getName());
			logger.error(format);
			throw new RuntimeException(format, e);
		}

		Map<String, Field> analyzeClazzInfo = analyzeClazzInfo(clazz);
		Set<Entry<String,Field>> entrySet = analyzeClazzInfo.entrySet();
		for(Entry<String,Field> entry : entrySet){
			String fieldName = entry.getKey();
			if(disassemblyWorker.getValue(source, fieldName)==null) continue;
			Field field = entry.getValue();
			Class<?> fieldType = field.getType();
			field.setAccessible(true);
			try {
				// 基础数据还原
				if(ParameterizedTypeUtil.isDirectSerializableType(fieldType))
					field.set(newInstance, drectSerializableTypeRevert(ParameterizedTypeUtil.getPrimitiveObjectType(fieldType), disassemblyWorker.getValue(source, fieldName)));
				// 枚举还原
				else if(fieldType.isEnum()) {
					Object value = disassemblyWorker.getValue(source, fieldName);
					if(value.getClass()==int.class || value.getClass()==Integer.class)
						field.set(newInstance, revertIntoEnumType(fieldType, (Integer )value));
					else
						field.set(newInstance, revertIntoEnumType(fieldType, (String )value));
				}
				//field.set(newInstance, revertIntoEnumType(fieldType, (Integer )disassemblyWorker.getValue(source, fieldName)));
				// Java集合类型
				else if (ParameterizedTypeUtil.hasSublevel(fieldType)) {
					if(Map.class.isAssignableFrom(fieldType))
						field.set(newInstance, disassemblyWorker.convertNodeAsMap(disassemblyWorker.getChildKVP(source, fieldName)));
					else if(Set.class.isAssignableFrom(fieldType))
						field.set(newInstance, disassemblyWorker.convertNodeAsSet(disassemblyWorker.getChildVP(source, fieldName)));
					else if(List.class.isAssignableFrom(fieldType))
						field.set(newInstance, disassemblyWorker.convertNodeAsList(disassemblyWorker.getChildVP(source, fieldName)));
				}
				// 数组
				else if(fieldType.isArray()) {
					ContainerVP vpNode = disassemblyWorker.getChildVP(source, fieldName);
					Class<?> componentType = fieldType.getComponentType();
					if(componentType==int.class)
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
					else 
						field.set(newInstance, revertIntoArray(componentType, vpNode));
				}
				// 常规对象
				else 
					field.set(newInstance, revert(fieldType, disassemblyWorker.getChildKVP(source, fieldName)));
			} catch (Exception e) {
				throw new RuntimeException(String.format("Revert faild on [%s]'s field [%s]", clazz.getName(), fieldName), e);
			}
		}

		return newInstance;
	}

	/**
	 * 还原枚举类型，通过枚举Index
	 */
	private <TEnum> TEnum revertIntoEnumType(Class<TEnum> enumType, int index){
		if(enumType.isEnum())
			return enumType.getEnumConstants()[index];
		throw new RuntimeException(String.format("[%s] is not a Enum type!!!", enumType.getName()));
	}

	/**
	 * 还原枚举类型，通过枚举的表现字符值
	 */
	private <TEnum> TEnum revertIntoEnumType(Class<TEnum> enumType, String represent){
		if(enumType.isEnum()) {
			for(TEnum obj:enumType.getEnumConstants())
				if(obj.toString().equals(represent)) return obj;
		}
		throw new RuntimeException(String.format("[%s] is not a Enum type!!!", enumType.getName()));
	}

	/**
	 * 在回显对象过程中，会可能出现类型匹配不上的情况。
	 * @param fieldType
	 * @param rawValue
	 * @return
	 */
	private Object drectSerializableTypeRevert(Class<?> fieldType, Object rawValue){
		Class<?> valueType = rawValue.getClass();
		if(fieldType==valueType)
			return rawValue;
		else if(fieldType==Long.class && ParameterizedTypeUtil.getPrimitiveObjectType(valueType)==Integer.class)
			return 0l+(Integer )rawValue;
		else if(fieldType==Integer.class && ParameterizedTypeUtil.getPrimitiveObjectType(valueType)==Long.class) 
			return Integer.parseInt(rawValue.toString());
		return rawValue;
	}


	/**
	 * 回显数组。
	 * TODO: 未测试！！！
	 * @param arrayType
	 * @param valueArray
	 * @return
	 */
	public <TComponent> TComponent[] revertIntoArray(Class<TComponent> componentType, ContainerVP vpNode) {
		int size = disassemblyWorker.getVPSize(vpNode);
		TComponent[] rArray = (TComponent[] )Array.newInstance(componentType, size);
		if(ParameterizedTypeUtil.isDirectSerializableType(componentType))
			for(int i=0; i<size; i++)
				rArray[i] = (TComponent )disassemblyWorker.getValue(vpNode, i);
		else if(ParameterizedTypeUtil.hasSublevel(componentType))
			throw new RuntimeException("Unsupport revert Java Collection/Map in Java Array!!!");
		else if(componentType.isEnum())
			throw new RuntimeException("Unsupport revert Java enum in Java Array!!!");
		else if(componentType.isArray()) {
			Class<?> childComponentType = componentType.getComponentType();
			if(childComponentType==int.class)
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
			else
				for(int i=0; i<size; i++)
					rArray[i] = (TComponent )revertIntoArray(childComponentType, disassemblyWorker.getChildVP(vpNode, i));
		} else
			for(int i=0; i<size; i++)
				rArray[i] = revert(componentType, disassemblyWorker.getChildKVP(vpNode, i));
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
		for(int i=0; i<size; i++)
			rArray[i] = (Byte )disassemblyWorker.getValue(vpNode, i);
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
}
