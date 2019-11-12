package pro.jiefzz.ejoker.z.system.enhance;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.system.functional.IFunction2;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction2;

/**
 * 在jdk1.8之后集合接口上都已经准备了 forEach方法了，大可用集合接口上的forEach方法 <br />
 * 而且java的stream/parallelStream越来越成熟和好用了。 <br />
 * 
 * @author kimffy
 *
 */
public class ForEachUtil {

	public static <K, V> void processForEach(Map<K, V> targetMap, IVoidFunction2<K, V> vf) {
		if(null == targetMap || targetMap.isEmpty())
			return;
		Set<Entry<K,V>> entrySet = targetMap.entrySet();
		for(Entry<K,V> entry : entrySet)
			vf.trigger(entry.getKey(), entry.getValue());
	}
	
	public static <K, V> void processForEachAndEliminate(Map<K, V> targetMap, IFunction2<Boolean, K, V> prediction) {
		processForEachAndEliminate(targetMap, null, prediction);
	}

	public static <K, V> void processForEachAndEliminate(Map<K, V> targetMap, IVoidFunction2<K, V> vf, IFunction2<Boolean, K, V> prediction) {
		if(null == targetMap || targetMap.isEmpty())
			return;
		Iterator<Entry<K, V>> iterator = targetMap.entrySet().iterator();
		if(null != vf)
			while(iterator.hasNext()) {
				Entry<K, V> current = iterator.next();
				K key = current.getKey();
				V value = current.getValue();
				vf.trigger(key, value);
				if(prediction.trigger(key, value))
					iterator.remove();
			}
		else
			while(iterator.hasNext()) {
				Entry<K, V> current = iterator.next();
				K key = current.getKey();
				V value = current.getValue();
				if(prediction.trigger(key, value))
					iterator.remove();
			}
	}
	
	public static <V> void processForEach(List<V> targetList, IVoidFunction1<V> vf) {
		if(null == targetList || targetList.isEmpty())
			return;
		for(V item : targetList)
			vf.trigger(item);
	}

	public static <V> void processForEachAndEliminate(List<V> targetList, IFunction1<Boolean, V> prediction) {
		processForEachAndEliminate(targetList, null, prediction);
	}
	
	public static <V> void processForEachAndEliminate(List<V> targetList, IVoidFunction1<V> vf, IFunction1<Boolean, V> prediction) {
		if(null == targetList || targetList.isEmpty())
			return;
		Iterator<V> iterator = targetList.iterator();
		if(null != vf)
			while(iterator.hasNext()) {
				V current = iterator.next();
				vf.trigger(current);
				if(prediction.trigger(current))
					iterator.remove();
			}
		else
			while(iterator.hasNext()) {
				V current = iterator.next();
				if(prediction.trigger(current))
					iterator.remove();
			}
	}
	
	public static <V> void processForEach(Set<V> targetList, IVoidFunction1<V> vf) {
		if(null == targetList || targetList.isEmpty())
			return;
		for(V item : targetList)
			vf.trigger(item);
	}

	public static <V> void processForEachAndEliminate(Set<V> targetList, IFunction1<Boolean, V> prediction) {
		processForEachAndEliminate(targetList, null, prediction);
	}
	
	public static <V> void processForEachAndEliminate(Set<V> targetList, IVoidFunction1<V> vf, IFunction1<Boolean, V> prediction) {
		if(null == targetList || targetList.isEmpty())
			return;
		Iterator<V> iterator = targetList.iterator();
		if(null != vf)
			while(iterator.hasNext()) {
				V current = iterator.next();
				vf.trigger(current);
				if(prediction.trigger(current))
					iterator.remove();
			}
		else
			while(iterator.hasNext()) {
				V current = iterator.next();
				if(prediction.trigger(current))
					iterator.remove();
			}
	}

	public static <V> void processForEach(V[] target, IVoidFunction2<V, Integer> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++)
			vf.trigger(target[i], i);
	}

	public static <V> void processForEach(V[] target, IVoidFunction1<V> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++)
			vf.trigger(target[i]);
	}
	
	public static <V> List<V> processSelect(V[] target, IFunction1<Boolean, V> prediction) {
		List<V> resList = new LinkedList<>(); // Why use LinkedList? Cause we just consider the write speed.
		if(null == target || 0 == target.length)
			return resList;
		for(int i = 0; i< target.length; i++)
			if(prediction.trigger(target[i]))
				resList.add(target[i]);
		return resList;
	}

	public static <V> List<V> processEliminate(V[] target, IFunction1<Boolean, V> prediction) {
		List<V> resList = new LinkedList<>(); // Why use LinkedList? Cause we just consider the write speed.
		if(null == target || 0 == target.length)
			return resList;
		for(int i = 0; i< target.length; i++)
			if(!prediction.trigger(target[i]))
				resList.add(target[i]);
		return resList;
	}
}
