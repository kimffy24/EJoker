package com.jiefzz.ejoker.z.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * 泛型工具类
 * @author jiefzz
 *
 */
public class GenericTypeUtil {

	/**
	 * 没有声明泛型签名时的jvm对泛型类型表现出的泛型签名<br>
	 * 多用于判断
	 */
	public final static String NO_GENERAL_SIGNATURE="";
	
	public final static String SEPARATOR=", ";
	
	/**
	 * 获取java类属性上声明的对象的泛型签名<br>
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
	public static String getDeclaredGenericSignature(final Field field){
		// 关键的地方得到其Generic的类型
		Type fc = field.getGenericType();
		String typeName = fc.getTypeName();
		int index = typeName.indexOf('<');
		if(index<=0) return NO_GENERAL_SIGNATURE;
		return typeName.substring(index);
	}
	
//	/**
//	 * 确定Type包装的类型是否是泛型类型
//	 * @param type
//	 * @return
//	 */
//	public static boolean ensureIsGenericTypex(final Type type) {
//		return type.getTypeName().indexOf('<')>0;
//	}
	
	/**
	 * 确定 类 是否是 泛型类
	 * @param clazz
	 * @return
	 */
	public static boolean ensureClassIsGenericType(final Class<?> clazz){
		return getTypeVariableAmount(clazz)>0?true:false;
	}
	
	/**
	 * 获取 类 的 泛型类型变量 的 数量。
	 * @param clazz
	 * @return
	 */
	public static int getTypeVariableAmount(final Class<?> clazz) {
		TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
		return typeParameters.length;
	}
	
	/**
	 * 获取对象的泛型签名。
	 * @param clazz
	 * @return
	 */
	public static String getClassDefinationGenericSignature(final Class<?> clazz) {
		TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
		StringBuffer sb = new StringBuffer();
		if(typeParameters!=null && typeParameters.length>0) {
			sb.append('<');
			sb.append(typeParameters[0].getTypeName());
			for(int i=1; i<typeParameters.length; i++) {
				TypeVariable<?> tv = typeParameters[i];
				sb.append(SEPARATOR);
				sb.append(tv.getTypeName());
			}
			sb.append('>');
			return sb.toString();
		}
		return NO_GENERAL_SIGNATURE;
	}

	/**
	 * 获取给定Java Type类型的泛型签名
	 * @param type
	 * @return
	 */
	public static String getClassDefinationGenericSignature(final Type type) {
		String typeName = type.getTypeName();
		int index = typeName.indexOf('<');
		if(index<=0) return NO_GENERAL_SIGNATURE;
		return typeName.substring(index);
	}

//	/**
//	 * 获取父类中整个继承树中的泛型对象
//	 * @param clazz
//	 * @return
//	 */
//	public static synchronized Map<String, String> getClassSuperGenericSignatureTreex(final Class<?> clazz) {
//		Map<String, String> superGenericSignatureMapper = new HashMap<String, String>();
//		for ( Class<?> clayy = clazz; clayy != Object.class; clayy = clayy.getSuperclass() ) {
//			Type genericAbstractClass = clayy.getGenericSuperclass();
//			Type[] genericInterfaces = clayy.getGenericInterfaces();
//			Type genericSuperClass;
//			for (int i=0; i<genericInterfaces.length+1; i++) {
//				if(i==0) genericSuperClass = genericAbstractClass;
//				else genericSuperClass = genericInterfaces[i-1];
//				if(ensureIsGenericType(genericSuperClass)) {
//					String typeName = genericSuperClass.getTypeName();
//					if(!ensureIsGenericType(genericSuperClass)) superGenericSignatureMapper.put(typeName, NO_GENERAL_SIGNATURE);
//					String genericSuperName = typeName.substring(0, typeName.indexOf('<'));
//					String classDefinationGenericSignature = getClassDefinationGenericSignature(genericSuperClass);
//					if(superGenericSignatureMapper.containsKey(genericSuperName)) {
//						if(!superGenericSignatureMapper.get(genericSuperName).equals(classDefinationGenericSignature))
//							throw new RuntimeException(clazz.getName() +" inherit " +genericSuperName +" more than once and with the difference Generic Signature!!!");
//					} else {
//						superGenericSignatureMapper.put(genericSuperName, classDefinationGenericSignature);
//					}
//				}
//			}
//		}
//		return superGenericSignatureMapper;
//	}
	
	
	
	/**
	 * 参考的空的泛型(未指定类型时)的泛型签名表
	 */
	public final static Map<Integer, String> emptyParametersBook = new HashMap<Integer, String>();

	/**
	 * 允许接受的最大长度泛型签名数量
	 */
	public final static int patametersAmountLimit = 16;
	
	static {
		String unsetType = Object.class.getName();
		StringBuilder sb = new StringBuilder(unsetType);
		emptyParametersBook.put(1, unsetType);
		for(int i=2; i<=patametersAmountLimit; i++) {
			sb.append(GenericTypeUtil.SEPARATOR);
			sb.append(unsetType);
			emptyParametersBook.put(i, sb.toString());
		}
	}
}
