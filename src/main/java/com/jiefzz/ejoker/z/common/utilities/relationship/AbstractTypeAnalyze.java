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
	
	public final static Map<String, Field> defaultEmptyInfo = new HashMap<String, Field>();				// 空的分析结果对象
	
	protected final static Map<Class<?>, Map<String, Field>> clazzRefectionInfo = new HashMap<Class<?>, Map<String, Field>>();
	
	private Lock lock4analyze = new ReentrantLock();
	
	protected <T> Map<String, Field> analyzeClazzInfo(final Class<T> clazz){

		if(ParameterizedTypeUtil.hasSublevel(clazz) || ParameterizedTypeUtil.isDirectSerializableType(clazz) || clazz.isArray() || clazz.isEnum())
			return null;
		
		lock4analyze.lock();
		try {
			Map<String, Field> analyzeResult = clazzRefectionInfo.getOrDefault(clazz, defaultEmptyInfo);
			if(analyzeResult!=defaultEmptyInfo)
				return analyzeResult;
			analyzeResult = new HashMap<String, Field>();
			clazzRefectionInfo.putIfAbsent(clazz, analyzeResult);
			Set<String> ignoreFiledName = new HashSet<String>();
			for ( Class<?> claxx = clazz; claxx != Object.class || claxx.isAnnotationPresent(PersistentTop.class); claxx = claxx.getSuperclass() ) {
				Field[] fields = claxx.getDeclaredFields();
				for ( Field field : fields ) {
					String fieldName = field.getName();
					if (fieldName.length()>=5 && "this$".equals(fieldName.substring(0, 5))) continue;
																				// skip parent class reference while here is a anonymous class
					int modifierBit = field.getModifiers();
					if(field.isAnnotationPresent(PersistentIgnore.class)		// declare ignore by @PersistentIgnore
							||Modifier.isStatic(modifierBit)					// final field
							||Modifier.isFinal(modifierBit)						// static field
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
