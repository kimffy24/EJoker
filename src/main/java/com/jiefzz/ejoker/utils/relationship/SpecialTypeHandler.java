package com.jiefzz.ejoker.utils.relationship;

import java.util.HashMap;
import java.util.Map;

/**
 * 为用户指定的类型提供解析器主持对象<br>
 * 对继承类型不友好，仅仅支持同类型入同类型出，不支持其父子类型的转换，请注意使用
 * @author kimffy
 *
 */
public class SpecialTypeHandler<TDistType> {
	
	private final Map<Class<?>, Handler<?, TDistType>> handlers;
	
	public SpecialTypeHandler() {
		handlers = new HashMap<Class<?>, Handler<?, TDistType>>();
	}
	
	public <TSpecType> SpecialTypeHandler append(Class<TSpecType> clazz, Handler<TSpecType, TDistType> handler) {
		if(null != handlers.putIfAbsent(clazz, handler))
			throw new RuntimeException(String.format("%s has more than one handler!!!", clazz.getName()));
		return this;
	}
	
	public <TSpecType> Handler<TSpecType, TDistType> getHandler(Class<TSpecType> clazz) {
		return (Handler<TSpecType, TDistType> )handlers.getOrDefault(clazz, null);
	}

	/**
	 * 解析器接口对象
	 * @author kimffy
	 *
	 * @param <TSpecType>
	 */
	public static interface Handler<TSpecType, TDistType> {
		
		public TDistType convert(TSpecType target);
		
		public TSpecType revert(TDistType source);
		
	}
}
