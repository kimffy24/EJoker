package com.jiefzz.ejoker.z.common.utils.relationship;

import java.util.Set;

public interface IRelationshipTreeDisassemblers<KVP, VP> {
	
	/**
	 * 从KVP中获取对应key的值
	 * @param source
	 * @param key
	 * @return
	 */
	public Object getValue(KVP source, Object key);

	/**
	 * 从VP中获取对应下标、位置的值
	 * @param source
	 * @param key
	 * @return
	 */
	public Object getValue(VP source, int index);

	/**
	 * 获取VP的长度。
	 * @param source
	 * @return
	 */
	public int getVPSize(VP source);
	
	/**
	 * 获取健值集的健集
	 * @param source
	 * @return
	 */
	public Set getKeySet(KVP source);
	
}
