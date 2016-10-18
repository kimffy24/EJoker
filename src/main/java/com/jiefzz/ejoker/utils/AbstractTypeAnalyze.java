package com.jiefzz.ejoker.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentTop;

public abstract class AbstractTypeAnalyze {
	
	private final static Logger logger = LoggerFactory.getLogger(AbstractTypeAnalyze.class);
	
	public final static Map<String, Field> defaultEmptyInfo = new HashMap<String, Field>();				// 空的分析结果对象
	
	protected final static Map<Class<?>, Map<String, Field>> clazzRefectionInfo = new HashMap<Class<?>, Map<String, Field>>();
	
	protected <T> Map<String, Field> analyzeClazzInfo(final Class<T> clazz){

		if(ParameterizedTypeUtil.hasSublevel(clazz) || ParameterizedTypeUtil.isDirectSerializableType(clazz) || clazz.isArray() || clazz.isEnum())
			return null;
		
		Map<String, Field> analyzeResult = clazzRefectionInfo.getOrDefault(clazz, defaultEmptyInfo);
		if(analyzeResult!=defaultEmptyInfo) return analyzeResult;
		analyzeResult = new HashMap<String, Field>();
		Set<String> ignoreFiledName = new HashSet<String>();
		for ( Class<?> claxx = clazz; claxx != Object.class || claxx.isAnnotationPresent(PersistentTop.class); claxx = claxx.getSuperclass() ) {
			Field[] fields = claxx.getDeclaredFields();
			for ( Field field : fields ) {
				String fieldName = field.getName();
				int modifierBit = field.getModifiers();
				if (fieldName.length()>=5 && "this$".equals(fieldName.substring(0, 5))) continue;
																			// skip parent class reference while here is a anonymous class
				if(field.isAnnotationPresent(PersistentIgnore.class)		// declare ignore by @PersistentIgnore
						||Modifier.isStatic(modifierBit)					// final field
						||Modifier.isFinal(modifierBit)						// static field
						||ignoreFiledName.contains(fieldName)				// has mark ignore in this function
						) {
					ignoreFiledName.add(fieldName);
					//logger.debug("{}.analyzeClazzInfo() ignore field [{}], while handle [{}]", AbstractTypeAnalyze.class.getName(), fieldName, clazz.getName());
					continue;
				}
				analyzeResult.putIfAbsent(field.getName(), field);
				analyzeClazzInfo(field.getType());
			}
		}
		clazzRefectionInfo.putIfAbsent(clazz, analyzeResult);
		return analyzeResult;
	}

}
