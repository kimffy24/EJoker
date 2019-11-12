package pro.jiefzz.ejoker.z.system.enhance;

import java.util.Map;

import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;

public final class MapUtil {

	/**
	 * 此方法并不负责线程安全，线程安全由具体的map实现类型负责
	 * @param <K>
	 * @param <T>
	 * @param map
	 * @param uniqueKey
	 * @param f
	 * @return
	 */
	public static <K, T> T getOrAdd(Map<K, T> map, K uniqueKey, IFunction<T> f) {
		T current, newOne = null;
		if(null == (current = map.get(uniqueKey))) {
			current = map.putIfAbsent(uniqueKey, newOne = f.trigger());
		}
		return null != current ? current : newOne;
	}

	/**
	 * 此方法并不负责线程安全，线程安全由具体的map实现类型负责
	 * @param <K>
	 * @param <T>
	 * @param map
	 * @param uniqueKey
	 * @param f
	 * @return
	 */
	public static <K, T> T getOrAdd(Map<K, T> map, K uniqueKey, IFunction1<T, K> f) {
		T current, newOne = null;
		if(null == (current = map.get(uniqueKey))) {
			current = map.putIfAbsent(uniqueKey, newOne = f.trigger(uniqueKey));
		}
		return null != current ? current : newOne;
	}
	
}
