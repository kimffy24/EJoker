package com.jiefzz.ejoker.z.common.utilities;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;

public final class ForEachUtil {

	public final static <K, V> void processForEach(Map<K, V> targetMap, IVoidFunction2<K, V> vf) {
		if(null == targetMap || 0 == targetMap.size())
			return;
		Set<Entry<K,V>> entrySet = targetMap.entrySet();
		for(Entry<K,V> entry : entrySet)
			vf.trigger(entry.getKey(), entry.getValue());
	}

	public final static <V> void processForEach(V[] target, IVoidFunction2<V, Integer> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++) {
			vf.trigger(target[i], i);
		}
	}

	public final static <V> void processForEach(V[] target, IVoidFunction1<V> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++) {
			vf.trigger(target[i]);
		}
	}
}
