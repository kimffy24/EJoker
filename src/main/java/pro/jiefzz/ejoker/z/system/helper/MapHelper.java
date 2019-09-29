package pro.jiefzz.ejoker.z.system.helper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.jiefzz.ejoker.z.system.functional.IFunction;

public final class MapHelper {
	
	public static <K, T> T getOrAddConcurrent(Map<K, T> map, K uniqueKey, IFunction<T> f) {
		if(map instanceof ConcurrentHashMap) {
			return getOrAdd(map, uniqueKey, f);
		}
		throw new RuntimeException("Cannot use getOrAddConcurrent on a map which is not instalce of ConcurrentHashMap!!!");
	}

	public static <K, T> T getOrAdd(Map<K, T> map, K uniqueKey, IFunction<T> f) {
		T value;
		while(null == (value = map.get(uniqueKey)))
			map.putIfAbsent(uniqueKey, f.trigger());
		return value;
	}
	
//	public static <K, T> T getOrAddConcurrent(Map<K, T> map, K uniqueKey, IFunction1<T, K> f) {
//		if(map instanceof ConcurrentHashMap) {
//			return getOrAdd(map, uniqueKey, f);
//		}
//		throw new RuntimeException("Cannot use getOrAddConcurrent on a map which is not instalce of ConcurrentHashMap!!!");
//	}
//
//	public static <K, T> T getOrAdd(Map<K, T> map, K uniqueKey, IFunction1<T, K> f) {
//		T value;
//		while(null == (value = map.get(uniqueKey)))
//			map.putIfAbsent(uniqueKey, f.trigger(uniqueKey));
//		return value;
//	}
	
}
