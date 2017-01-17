package com.jiefzz.ejoker.z.common.system.util.extension;

public final class KeyValuePair<TKey, TValue> {

	private TKey key = null;
	private TValue value = null;
	
	public TKey getKey() {
		return key;
	}
	public void setKey(TKey key) {
		this.key = key;
	}
	public TValue getValue() {
		return value;
	}
	public void setValue(TValue value) {
		this.value = value;
	}
	
	public KeyValuePair(){}
	public KeyValuePair(TKey key, TValue value) {
		this.key = key;
		this.value = value;
	}
}
