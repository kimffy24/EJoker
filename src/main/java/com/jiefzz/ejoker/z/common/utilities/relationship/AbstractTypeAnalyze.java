package com.jiefzz.ejoker.z.common.utilities.relationship;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentTop;

public abstract class AbstractTypeAnalyze {
	
	/**
	 * 空的分析结果对象
	 */
	public final static Map<String, Field> defaultEmptyInfo = new HashMap<String, Field>();
	
	/**
	 * 对每个类的反射分析信息都会记录到这个字典中。
	 */
	protected final static Map<Class<?>, Map<String, Field>> clazzRefectionInfo = new HashMap<Class<?>, Map<String, Field>>();
	
	private Lock lock4analyze = new ReentrantLock();
	
	protected <T> Map<String, Field> analyzeClazzInfo(final Class<T> clazz){

		// 对工具类、组合类、枚举不做反射分析记录
		if(ParameterizedTypeUtil.hasSublevel(clazz) || ParameterizedTypeUtil.isDirectSerializableType(clazz) || clazz.isArray() || clazz.isEnum())
			return null;
		
		Map<String, Field> analyzeResult;
		if( !defaultEmptyInfo.equals( analyzeResult = clazzRefectionInfo.getOrDefault(clazz, defaultEmptyInfo) ) )
			return analyzeResult;
		
		// 约定对象结构分析同时只能有一个线程进行。
		lock4analyze.lock();
		try {
			analyzeResult = new HashMap<String, Field>();
			if(null != clazzRefectionInfo.putIfAbsent(clazz, analyzeResult))
				return clazzRefectionInfo.get(clazz);
			Set<String> ignoreFiledName = new HashSet<String>();
			for ( Class<?> claxx = clazz; claxx != Object.class || claxx.isAnnotationPresent(PersistentTop.class); claxx = claxx.getSuperclass() ) {
				Field[] fields = claxx.getDeclaredFields();
				for ( Field field : fields ) {
					String fieldName = field.getName();
					if (fieldName.length()>=5 && fieldName.startsWith("this$"))	// skip parent class reference while here is a anonymous class
						continue;
					int modifierBit = field.getModifiers();
					if(field.isAnnotationPresent(PersistentIgnore.class)		// declare ignore by @PersistentIgnore
							||Modifier.isStatic(modifierBit)					// static field
							||Modifier.isFinal(modifierBit)						// final field
							||ignoreFiledName.contains(fieldName)				// has mark ignore in this function
							) {
						ignoreFiledName.add(fieldName);
						continue;
					}
					analyzeResult.putIfAbsent(field.getName(), field);
					analyzeClazzInfo(field.getType());
				}
			}
			return analyzeResult;
		} finally {
			lock4analyze.unlock();
		}
	}

}
