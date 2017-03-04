package com.jiefzz.ejoker.utils.relationship;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RevertRelationshipTreeDisassemblyInterface<KVP, VP> {
	
	/**
	 * 从某个KVP中获取其包含的KVP元素
	 * @param source
	 * @param key
	 * @return
	 */
	public KVP getChildKVP(KVP source, String key);
	
	/**
	 * 从VP中获取其包含的KVP元素
	 * @param source
	 * @param index
	 * @return
	 */
	public KVP getChildKVP(VP source, int index);
	
	/**
	 * 从某个KVP中获取其包含的VP元素
	 * @param source
	 * @param key
	 * @return
	 */
	public VP getChildVP(KVP source, String key);
	
	/**
	 * 从某个VP中获取其包含的VP元素
	 * @param source
	 * @param key
	 * @return
	 */
	public VP getChildVP(VP source, int index);
	
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
	 * 把KVP转化为java的Map
	 * @param source
	 * @return
	 */
	public Map convertNodeAsMap(KVP source);
	
	/**
	 * 把VP转化为java的Set
	 * @param source
	 * @return
	 */
	public Set convertNodeAsSet(VP source);
	
	/**
	 * 把VP转化为java的List
	 * @param source
	 * @return
	 */
	public List convertNodeAsList(VP source);
	
}
