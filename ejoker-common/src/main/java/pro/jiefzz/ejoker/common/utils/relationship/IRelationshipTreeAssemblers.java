package pro.jiefzz.ejoker.common.utils.relationship;

public interface IRelationshipTreeAssemblers<KVP, VP> {
	
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
	
}
