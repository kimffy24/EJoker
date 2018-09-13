package com.jiefzz.ejoker.z.common.utils;

import java.util.List;
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
	
	public final static <V> void processForEach(List<V> targetList, IVoidFunction1<V> vf) {
		if(null == targetList || 0 == targetList.size())
			return;
		for(V item : targetList)
			vf.trigger(item);
	}
	
	public final static <V> void processForEach(List<V> targetList, IVoidFunction2<V, Integer> vf) {
		if(null == targetList || 0 == targetList.size())
			return;
		int size = targetList.size();
		for(int i = 0; i<size; i++ )
			vf.trigger(targetList.get(i), i);
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
