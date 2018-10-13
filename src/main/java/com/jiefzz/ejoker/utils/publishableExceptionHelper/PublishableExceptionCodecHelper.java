package com.jiefzz.ejoker.utils.publishableExceptionHelper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.z.common.context.dev2.EJokerInstanceBuilder;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;
import com.jiefzz.ejoker.z.common.utils.ParameterizedTypeUtil;

public final class PublishableExceptionCodecHelper {
	
	private static Map<Class<? extends IPublishableException>, Map<String, Field>> reflectMap= new HashMap<>();

	public static Map<String, String> serialize(IPublishableException exception) {
		Map<String, String> rMap = new HashMap<>();
		
		Map<String, Field> reflectFields = getReflectFields(exception.getClass());
		ForEachUtil.processForEach(reflectFields, (n, f) -> {

			Object fValue;
			try {
				fValue = f.get(exception);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			rMap.put(n, sValue(f.getType(), fValue));
			
		});
		
		return rMap;
	}
	
	public static IPublishableException deserialize(Map<String, String> pMap, Class<? extends IPublishableException> exceptionClazz) {
		
		return (IPublishableException )(new EJokerInstanceBuilder(exceptionClazz).doCreate((e) -> {
			
			Map<String, Field> reflectFields = getReflectFields(exceptionClazz);
			ForEachUtil.processForEach(reflectFields, (n, f) -> {
				
				Object dValue = dValue(f.getType(), pMap.get(n));
				try {
					f.set(e, dValue);
				} catch (Exception ex) {
					throw new RuntimeException(ex.getMessage(), ex);
				}
				
			});
			
		}));
		
	}
	
	public static Map<String, Field> getReflectFields(Class<? extends IPublishableException> exceptionClazz)  {
		return MapHelper.getOrAdd(reflectMap, exceptionClazz, () -> {
			
			Map<String, Field> rMap = new HashMap<>();
			
			for(Class<?> current = exceptionClazz;
					!RuntimeException.class.equals(current);
					current = current.getSuperclass()) {
				
				ForEachUtil.processForEach(current.getDeclaredFields(), (field) -> {
					
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();
					
					if(!ParameterizedTypeUtil.isDirectSerializableType(fieldType))
						throw new RuntimeException(String.format("Unsupport non-basic field in PublishableException!!! type: %s, field: %s", exceptionClazz.getName(), fieldName));
					
					field.setAccessible(true);
					rMap.putIfAbsent(fieldName, field);
					
				});
				
				
			}
			
			return rMap;
		});
	}
	
	private static String sValue(Class<?> type, Object value) {

		if(int.class.equals(type) || Integer.class.equals(type)) {
			return "" + value;
		} else if(long.class.equals(type) || Long.class.equals(type)) {
			return "" + value;
		} else if(short.class.equals(type) || Short.class.equals(type)) {
			return "" + value;
		} else if(char.class.equals(type) || Character.class.equals(type)) {
			return "" + value;
		} else if(float.class.equals(type) || Float.class.equals(type)) {
			return "" + value;
		} else if(double.class.equals(type) || Double.class.equals(type)) {
			return "" + value;
		} else if(byte.class.equals(type) || Byte.class.equals(type)) {
			return "" + ((int )value);
		} else if(boolean.class.equals(type) || Boolean.class.equals(type)) {
			return ((boolean )value) ? "true" : "false";
		} else if(String.class.equals(type)) {
			return (String )value;
		}
		throw new RuntimeException(String.format("Unsupport type[%s] on PublishableException!!!", type.getName()));
	}

	private static Object dValue(Class<?> type, String tValue) {

		if(int.class.equals(type) || Integer.class.equals(type)) {
			return Integer.valueOf(tValue);
		} else if(long.class.equals(type) || Long.class.equals(type)) {
			return Long.valueOf(tValue);
		} else if(short.class.equals(type) || Short.class.equals(type)) {
			return Short.valueOf(tValue);
		} else if(char.class.equals(type) || Character.class.equals(type)) {
			return Character.valueOf((char )(Integer.valueOf(tValue).intValue()));
		} else if(float.class.equals(type) || Float.class.equals(type)) {
			return Float.valueOf(tValue);
		} else if(double.class.equals(type) || Double.class.equals(type)) {
			return Double.valueOf(tValue);
		} else if(byte.class.equals(type) || Byte.class.equals(type)) {
			return Byte.valueOf(tValue);
		} else if(boolean.class.equals(type) || Boolean.class.equals(type)) {
			return Boolean.valueOf(tValue);
		} else if(String.class.equals(type)) {
			return tValue;
		}
		throw new RuntimeException(String.format("Unsupport type[%s] on PublishableException!!!", type.getName()));
	}
}
