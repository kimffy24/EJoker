package com.jiefzz.ejoker.z.common.utils.genericity;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class GenericDefinedField {

	public final String fiendName;

	public final String genericTypeVariableName;
	
	public final boolean isGenericVariable;

	public final Field field;

	public final GenericDefination genericDefination;
	
	public final GenericDefinedTypeMeta genericDefinedTypeMeta;

	public GenericDefinedField(GenericDefination genericDefination, Field field) {
		
		if (genericDefination.isInterface)
			throw new RuntimeException(String.format("We didn't access any field on an interface!!! defination=%s",
					genericDefination.genericPrototypeClazz.getName()));
		if(!field.getDeclaringClass().equals(genericDefination.genericPrototypeClazz))
			throw new RuntimeException(String.format("Wrong relationship!!! genericPrototype=%s, fieldDeclaringClass=%s",
					genericDefination.genericPrototypeClazz.getName(), field.getDeclaringClass().getName()));

		Type fieldType = field.getGenericType();
		this.isGenericVariable = fieldType instanceof TypeVariable ? true : false;
		this.genericDefination = genericDefination;
		this.field = field;
		this.fiendName = field.getName();
		
		if(isGenericVariable) {
			this.genericDefinedTypeMeta = null;
			this.genericTypeVariableName = fieldType.getTypeName();
		} else {
			this.genericDefinedTypeMeta = new GenericDefinedTypeMeta(fieldType, genericDefination);
			this.genericTypeVariableName = null;
		}
		
		field.setAccessible(true);
	}
	
	/**
	 * 复制构造，并更新GenericDefinedTypeMeta的信息
	 * @param regionGenericDefinedField
	 * @param genericDefinedTypeMeta
	 */
	public GenericDefinedField(GenericDefinedField regionGenericDefinedField, GenericDefinedTypeMeta genericDefinedTypeMeta) {
		this.isGenericVariable = regionGenericDefinedField.isGenericVariable;
		this.genericDefination = regionGenericDefinedField.genericDefination;
		this.field = regionGenericDefinedField.field;
		this.fiendName = regionGenericDefinedField.fiendName;
		this.genericTypeVariableName = regionGenericDefinedField.genericTypeVariableName;
		this.genericDefinedTypeMeta = genericDefinedTypeMeta;
	}
}
