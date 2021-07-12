package pro.jk.ejoker.common.utils.genericity;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * 
 * Represents a meta object about the GenericDeclared field attached on some GenericDefination.<br />
 * includes the relationship between GenericClass and GenericDeclared field. <br />
 * <br /><br />
 * 
 * 描述类的字段信息，包含完整的泛型信息，以及与定义类的泛型变量的映射
 * 
 * @author kimffy
 *
 */
public class GenericDefinedField extends GenericDefinitionElement {

	/**
	 * 属性名
	 */
	public final String fiendName;

	/**
	 * 属性的反射对象
	 */
	public final Field field;
	
	/**
	 * 属性的类型是否是一个泛型类型 (注意: 如 private Map&lt;String, T&gt; m; 带泛型的具体类型，不看作泛型类型)
	 */
	public final boolean isGenericVariable;

	/**
	 * 泛型类型变量的名字<br />
	 * eg: 定义<br />
	 * 	private T id;<br />
	 * 则取 T <br />
	 * 如果类型不是泛型，则取null (包括 private Map&lt;String, T&gt; m; 带泛型的具体类型，也取null) <br />
	 * 当 {@link GenericDefinedField#isGenericVariable} = true 时，此字段有效
	 */
	public final String genericTypeVariableName;

	/**
	 * <br />
	 * 当 {@link GenericDefinedField#isGenericVariable} = false 时，此字段有效
	 */
	public final GenericDefinedType genericDefinedType;

	public GenericDefinedField(GenericDefinition genericDefination, Field field) {
		super(genericDefination);
		
		if (genericDefination.isInterface)
			throw new RuntimeException(String.format("We didn't access any field on an interface!!! defination=%s",
					genericDefination.genericPrototypeClazz.getName()));
		if(!field.getDeclaringClass().equals(genericDefination.genericPrototypeClazz))
			throw new RuntimeException(String.format("Wrong relationship!!! genericPrototype=%s, fieldDeclaringClass=%s",
					genericDefination.genericPrototypeClazz.getName(),
					field.getDeclaringClass().getName()));

		Type fieldType = field.getGenericType();
		this.isGenericVariable = fieldType instanceof TypeVariable ? true : false;
		this.field = field;
		this.fiendName = field.getName();
		
		if(isGenericVariable) {
			this.genericDefinedType = null;
			this.genericTypeVariableName = fieldType.getTypeName();
		} else {
			this.genericDefinedType = new GenericDefinedType(fieldType, genericDefination);
			this.genericTypeVariableName = null;
		}
		
		field.setAccessible(true);
	}
	
	/**
	 * 复制构造，并替换掉GenericDefinedType为新的genericDefinedTypeMeta
	 * @param regionGenericDefinedField
	 * @param genericDefinedTypeMeta
	 */
	public GenericDefinedField(GenericDefinedField regionGenericDefinedField, GenericDefinedType genericDefinedTypeMeta) {
		super(regionGenericDefinedField.referDefination);
		this.isGenericVariable = regionGenericDefinedField.isGenericVariable;
		this.field = regionGenericDefinedField.field;
		this.fiendName = regionGenericDefinedField.fiendName;
		this.genericTypeVariableName = regionGenericDefinedField.genericTypeVariableName;
		this.genericDefinedType = genericDefinedTypeMeta;
	}
}
