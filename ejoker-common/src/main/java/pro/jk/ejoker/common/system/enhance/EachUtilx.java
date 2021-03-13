package pro.jk.ejoker.common.system.enhance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.system.functional.IFunction2;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;

/**
 * 在jdk1.8之后集合接口上都已经准备了 forEach方法了，大可用集合接口上的forEach方法 <br />
 * 而且java的stream/parallelStream越来越成熟和好用了。 <br />
 * 如果使用quasar协程，那么这个工具可以提供更友好的quasar织入能力<br />
 * @author kimffy
 *
 */
public class EachUtilx {

	/**
	 * 对每一个项进行vf处理
	 * @param <K>
	 * @param <V>
	 * @param targetMap
	 * @param vf
	 */
	public static <K, V> void loop(Map<K, V> targetMap, IVoidFunction2<K, V> vf) {
		if(null == targetMap || targetMap.isEmpty())
			return;
		Set<Entry<K,V>> entrySet = targetMap.entrySet();
		for(Entry<K,V> entry : entrySet)
			vf.trigger(entry.getKey(), entry.getValue());
	}
	/**
	 * @see #loop(Map, IVoidFunction2)
	 * @param <K>
	 * @param <V>
	 * @param targetMap
	 * @param vf
	 * @deprecated 请改用loop系列方法
	 */
	public static <K, V> void forEach(Map<K, V> targetMap, IVoidFunction2<K, V> vf) {
		loop(targetMap, vf);
	}

	/**
	 * 对每一个项进行vf处理
	 * @param <V>
	 * @param target
	 * @param vf
	 */
	public static <V> void loop(Collection<V> target, IVoidFunction1<V> vf) {
		if(null == target || target.isEmpty())
			return;
		for(V item : target)
			vf.trigger(item);
	}
	/**
	 * @see #loop(Collection, IVoidFunction1)
	 * @param <V>
	 * @param target
	 * @param vf
	 * @deprecated 请改用loop系列方法
	 */
	public static <V> void forEach(Collection<V> target, IVoidFunction1<V> vf) {
		loop(target, vf);
	}

	/**
	 * 对每一个项进行vf处理
	 * @param <V>
	 * @param target
	 * @param vf
	 */
	public static <V> void loop(V[] target, IVoidFunction1<V> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++)
			vf.trigger(target[i]);
	}
	/**
	 * @see #loop(V[], IVoidFunction1)
	 * @param <V>
	 * @param target
	 * @param vf
	 * @deprecated 请改用loop系列方法
	 */
	public static <V> void forEach(V[] target, IVoidFunction1<V> vf) {
		loop(target, vf);
	}

	/**
	 * 对每一个项进行vf处理<br />
	 * @param <V>
	 * @param target
	 * @param vf 第一个参当前迭代到的元素，第二个参数是元素的序号
	 */
	public static <V> void loop(V[] target, IVoidFunction2<V, Integer> vf) {
		if(null == target || 0 == target.length)
			return;
		for(int i = 0; i< target.length; i++)
			vf.trigger(target[i], i);
	}
	/**
	 * @see #loop(V[], IVoidFunction2)
	 * @param <V>
	 * @param target
	 * @param vf
	 * @deprecated 请改用loop系列方法
	 */
	public static <V> void forEach(V[] target, IVoidFunction2<V, Integer> vf) {
		loop(target, vf);
	}
	
	/**
	 * 1. 对每一个项进行vf处理<br />
	 * 2. 剔除符合条件的目标项
	 * @param <K>
	 * @param <V>
	 * @param targetMap
	 * @param vf
	 * @param prediction
	 */
	public static <K, V> void loopAndEliminate(Map<K, V> targetMap, IVoidFunction2<K, V> vf, IFunction2<Boolean, K, V> prediction) {
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
				if(prediction.trigger(current.getKey(), current.getValue()))
					iterator.remove();
			}
	}
	/**
	 * @see #loopAndEliminate(Map, IVoidFunction2, IFunction2)
	 * @param <K>
	 * @param <V>
	 * @param targetMap
	 * @param vf
	 * @param prediction
	 * @deprecated 请改用loop系列方法
	 */
	public static <K, V> void forEachAndEliminate(Map<K, V> targetMap, IVoidFunction2<K, V> vf, IFunction2<Boolean, K, V> prediction) {
		loopAndEliminate(targetMap, vf, prediction);
	}
	
	/**
	 * 1. 对每一个项进行vf处理<br />
	 * 2. 剔除符合条件的目标项
	 * @param <V>
	 * @param targetList
	 * @param vf
	 * @param prediction
	 */
	public static <V> void loopAndEliminate(Collection<V> targetList, IVoidFunction1<V> vf, IFunction1<Boolean, V> prediction) {
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
				if(prediction.trigger(iterator.next()))
					iterator.remove();
			}
	}
	/**
	 * @see #loopAndEliminate(Collection, IVoidFunction1, IFunction1)
	 * @param <V>
	 * @param targetList
	 * @param vf
	 * @param prediction
	 * @deprecated 请改用loop系列方法
	 */
	public static <V> void forEachAndEliminate(Collection<V> targetList, IVoidFunction1<V> vf, IFunction1<Boolean, V> prediction) {
		loopAndEliminate(targetList, vf, prediction);
	}

	/**
	 * 剔除符合条件的目标项
	 * @param <K>
	 * @param <V>
	 * @param targetMap
	 * @param prediction
	 */
	public static <K, V> void eliminate(Map<K, V> targetMap, IFunction2<Boolean, K, V> prediction) {
		if(null == targetMap || targetMap.isEmpty())
			return;
		Iterator<Entry<K, V>> iterator = targetMap.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<K, V> current = iterator.next();
			if(prediction.trigger(current.getKey(), current.getValue()))
				iterator.remove();
		}
	}
	/**
	 * @see #eliminate(Map, IFunction2)
	 * @param <K>
	 * @param <V>
	 * @param targetMap
	 * @param prediction
	 * @deprecated
	 */
	public static <K, V> void forEachAndEliminate(Map<K, V> targetMap, IFunction2<Boolean, K, V> prediction) {
		eliminate(targetMap, prediction);
	}

	/**
	 * 剔除符合条件的目标项
	 * @param <V>
	 * @param targetList
	 * @param prediction
	 */
	public static <V> void eliminate(Collection<V> targetList, IFunction1<Boolean, V> prediction) {
		if(null == targetList || targetList.isEmpty())
			return;
		Iterator<V> iterator = targetList.iterator();
		while(iterator.hasNext()) {
			if(prediction.trigger(iterator.next()))
				iterator.remove();
		}
	}
	/**
	 * @see #eliminate(Collection, IFunction1)
	 * @param <V>
	 * @param targetList
	 * @param prediction
	 * @deprecated
	 */
	public static <V> void forEachAndEliminate(Collection<V> targetList, IFunction1<Boolean, V> prediction) {
		eliminate(targetList, prediction);
	}

	/**
	 * 在数组中选出符合条件的元素，并把结果组装成list
	 * @param <V>
	 * @param target
	 * @param prediction
	 * @return 返回是一个LinkedList实例
	 */
	public static <V> List<V> select(V[] target, IFunction1<Boolean, V> prediction) {
		if(null == target || 0 == target.length)
			return new ArrayList<>();
		List<V> resList = new LinkedList<>(); // Why use LinkedList? Cause we just consider the write speed.
		for(int i = 0; i< target.length; i++)
			if(prediction.trigger(target[i]))
				resList.add(target[i]);
		return resList;
	}

}
