package pro.jk.ejoker.common.utils.relationship;

import java.util.HashMap;
import java.util.Map;

/**
 * 为用户指定的类型提供解析器主持对象<br>
 * 对继承类型不友好，仅仅支持同类型入同类型出，不支持其父子类型的转换，请注意使用
 * @author kimffy
 *
 */
public class SpecialTypeCodecStore<TDistType> {
	
	private final Map<Class<?>, SpecialTypeCodec<?, TDistType>> handlers;
	
	public SpecialTypeCodecStore() {
		handlers = new HashMap<Class<?>, SpecialTypeCodec<?, TDistType>>();
	}
	
	public <TSpecType> SpecialTypeCodecStore append(Class<TSpecType> clazz, SpecialTypeCodec<TSpecType, TDistType> handler) {
		if(null != handlers.putIfAbsent(clazz, handler))
			throw new RuntimeException(String.format("%s has more than one handler!!!", clazz.getName()));
		return this;
	}
	
	public <TSpecType> SpecialTypeCodec<TSpecType, TDistType> getCodec(Class<TSpecType> clazz) {
		SpecialTypeCodec<TSpecType, TDistType> handler = (SpecialTypeCodec<TSpecType, TDistType> )handlers.getOrDefault(clazz, null);
		return handler;
	}

}
