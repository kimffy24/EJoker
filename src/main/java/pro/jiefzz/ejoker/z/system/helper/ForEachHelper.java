package pro.jiefzz.ejoker.z.system.helper;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction2;

import java.util.Set;

public class ForEachHelper {

	public static <K, V> void processForEach(Map<K, V> targetMap, IVoidFunction2<K, V> vf) {
		if(null == targetMap || 0 == targetMap.size())
			return;
		Set<Entry<K,V>> entrySet = targetMap.entrySet();
		for(Entry<K,V> entry : entrySet)
			vf.trigger(entry.getKey(), entry.getValue());
	}
	
	public static <V> void processForEach(List<V> targetList, IVoidFunction1<V> vf) {
		if(null == targetList || 0 == targetList.size())
			return;
		for(V item : targetList)
			vf.trigger(item);
	}
	
	public static <V> void processForEach(Set<V> targetList, IVoidFunction1<V> vf) {
		if(null == targetList || 0 == targetList.size())
			return;
		for(V item : targetList)
			vf.trigger(item);
	}
	
	public static <V> void processForEach(List<V> targetList, IVoidFunction2<V, Integer> vf) {
		if(null == targetList || 0 == targetList.size())
			return;
		// 尽量使用foreach和迭代器
		// 如果使用ArrayList可以直接使用顺序迭代( for(int i=0; i<list.size(); i++) )
		int index = 0;
		for(V item : targetList)
			vf.trigger(item, index++);
	}

	public static <V> void processForEach(V[] target, IVoidFunction2<V, Integer> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++) {
			vf.trigger(target[i], i);
		}
	}

	public static <V> void processForEach(V[] target, IVoidFunction1<V> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++) {
			vf.trigger(target[i]);
		}
	}
}
