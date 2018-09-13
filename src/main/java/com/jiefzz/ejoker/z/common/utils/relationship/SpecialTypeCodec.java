package com.jiefzz.ejoker.z.common.utils.relationship;

/**
 * 解析器接口对象
 * @author kimffy
 *
 * @param <TSpecType> 特定对象类型
 * @param <TDistType> 封装类型
 */
public interface SpecialTypeCodec<TSpecType, TDistType> {
	
	/**
	 * 编码为封装类型
	 * @param target
	 * @return
	 */
	public TDistType encode(TSpecType target);
	
	/**
	 * 解码为源类型
	 * @param source
	 * @return
	 */
	public TSpecType decode(TDistType source);
	
}