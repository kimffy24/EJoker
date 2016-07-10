package com.jiefzz.ejoker.z.common.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;

/**
 * 泛型工具类
 * @author jiefzz
 *
 */
public class GeneralTypeUtil {

	public final static String NO_GENERAL_SIGNATURE="";
	public final static String SEPARATOR="|";
	
	/**
	 * 获取对象属性的泛型签名
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
	 * 获取对象类型的泛型使用数量。
	 * @param clazz
	 * @return
	 */
	public static int getGeneralTypeAmount(Class<?> clazz) {
		TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
		if(null==typeParameters) return 0;
		if(typeParameters.length>patametersAmountLimit) throw new ContextRuntimeException("Unsupport Parameters amount over than "+patametersAmountLimit+"!!!");
		return typeParameters.length;
	}

	/**
	 * 参考的空的泛型(未指定类型时)的泛型签名表
	 */
	public final static Map<Integer, String> emptyParametersBook;

	public final static int patametersAmountLimit;
	
	static {
		patametersAmountLimit = 15;
		emptyParametersBook = new HashMap<Integer, String>();
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
