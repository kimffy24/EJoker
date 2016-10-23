package com.jiefzz.ejoker.z.common.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * 泛型工具类
 * @author jiefzz
 *
 */
public class GeneralTypeUtil {

	/**
	 * 没有声明泛型签名时的jvm对泛型类型表现出的泛型签名<br>
	 * 多用于判断
	 */
	public final static String NO_GENERAL_SIGNATURE="";
	public final static String SEPARATOR="|";
	
	/**
	 * 获取对象属性的泛型签名<br>
	 * TODO 当泛型不是直接声明时会出错<br>
	 * 例如<br>
	 * class A&lt;PType&gt; {<br>
	 * public void doSomething(PType param) { ...; }<br>
	 * }<br>
	 * class B&lt;PType2&gt; {<br>
	 * public A&lt;PType2&gt; a;<br>
	 * }<br>
	 * 此时在B类的field反射对象上获取a的泛型类型会出错<br>
	 * @param field
	 * @return
	 */
	public static String getGeneralSignature(Field field){
		Type fc = field.getGenericType();		// 关键的地方得到其Generic的类型
		// 如果不为空并且是泛型参数的类型
		if (fc != null && fc instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) fc;
			Type[] types = pt.getActualTypeArguments();
			
			StringBuffer sb = new StringBuffer();
			
			if (types != null && types.length > 0) {
				for (int i = 0; i < types.length; i++) {
					sb.append(SEPARATOR);
					// TODO 若此属性的泛型信息是通过引用类的泛型定义传入的话，此处转换将会出错！！！
					sb.append(((Class<?>) types[i]).getName());
				}
				return sb.toString().substring(1);
			}
		}
		return NO_GENERAL_SIGNATURE;
	}
	
	/**
	 * 确定对象类型是否是泛型
	 * @param clazz
	 * @return
	 */
	public static boolean ensureIsGeneralType(Class<?> clazz){
		return getGeneralTypeAmount(clazz)>0?true:false;
	}
	
	/**
	 * 确定Type包装的类型是否是泛型类型
	 * @param type
	 * @return
	 */
	public static boolean ensureIsGeneralType(Type type) {
		return type.getTypeName().indexOf('<')>0;
	}
	
	/**
	 * 获取对象类型的泛型使用数量。
	 * @param clazz
	 * @return
	 */
	public static int getGeneralTypeAmount(Class<?> clazz) {
		TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
		if(null==typeParameters) return 0;
		return typeParameters.length;
	}
	
	public static String getClassDefinationGeneralSignature(Class<?> clazz) {

		TypeVariable[] typeParameters = clazz.getTypeParameters();
		StringBuffer sb = new StringBuffer();
		if(typeParameters.length>0) {
			sb.append('<');
			sb.append(typeParameters[0].getTypeName());
			for(int i=1; i<typeParameters.length; i++) {
				TypeVariable tv = typeParameters[i];
				sb.append(", ");
				sb.append(tv.getTypeName());
			}
			sb.append('>');
			return sb.toString();
		}
		return "";
	}

	public static String getClassDefinationGeneralSignature(Type type) {
		if(!ensureIsGeneralType(type)) return "";
		String typeName = type.getTypeName();
		return typeName.substring(typeName.indexOf('<'));
	}

	/**
	 * 参考的空的泛型(未指定类型时)的泛型签名表
	 */
	public final static Map<Integer, String> emptyParametersBook = new HashMap<Integer, String>();

	/**
	 * 允许接受的最大长度泛型签名数量
	 */
	public final static int patametersAmountLimit = 15;
	
	static {
		String unsetType = Object.class.getName();
		StringBuilder sb = new StringBuilder(unsetType);
		emptyParametersBook.put(1, unsetType);
		for(int i=2; i<=patametersAmountLimit; i++) {
			sb.append(GeneralTypeUtil.SEPARATOR);
			sb.append(unsetType);
			emptyParametersBook.put(i, sb.toString());
		}
	}
}
