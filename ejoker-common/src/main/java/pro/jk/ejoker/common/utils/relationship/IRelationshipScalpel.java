package pro.jk.ejoker.common.utils.relationship;

import java.util.Set;

public interface IRelationshipScalpel<KVP, VP> {
	
	/**
	 * 创建一个键值集容器
	 * @return
	 * @throws Exception
	 */
	public KVP createKeyValueSet();
	
	/**
	 * 创建一个值集容器
	 * @return
	 * @throws Exception
	 */
	public VP createValueSet();
	
	/**
	 * 判断容器中是否存在该键对应的值，仅仅对键值集有效
	 * @param targetNode
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public boolean isHas(KVP targetNode, Object key);
	
	/**
	 * 添加元素到值集的方法
	 * @param valueSet
	 * @param child
	 * @throws Exception
	 */
	public void addToValueSet(VP valueSet, Object child);
	
	/**
	 * 添加元素到键值集的方法
	 * @param keyValueSet
	 * @param child
	 * @param key
	 * @throws Exception
	 */
	public void addToKeyValueSet(KVP keyValueSet, Object child, String key);

	/**
	 * 从键值集合容器中取出一个某个键对应的值
	 * @param targetNode
	 * @param key
	 * @return
	 */
	public Object getFromKeyValeSet(KVP targetNode, Object key);

	/**
	 * 有这个key不？
	 * @param source
	 * @param key
	 * @return
	 */
	public boolean hasKey(KVP source, Object key);

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
