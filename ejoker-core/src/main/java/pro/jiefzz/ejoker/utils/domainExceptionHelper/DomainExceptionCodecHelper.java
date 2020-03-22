package pro.jiefzz.ejoker.utils.domainExceptionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import pro.jiefzz.ejoker.common.context.annotation.persistent.PersistentIgnore;
import pro.jiefzz.ejoker.common.context.dev2.EJokerInstanceBuilder;
import pro.jiefzz.ejoker.common.system.enhance.EachUtilx;
import pro.jiefzz.ejoker.common.system.enhance.MapUtilx;
import pro.jiefzz.ejoker.common.utils.SerializableCheckerUtil;
import pro.jiefzz.ejoker.domain.domainException.IDomainException;

public final class DomainExceptionCodecHelper {
	
	private static Map<Class<? extends IDomainException>, Map<String, Field>> reflectMap= new HashMap<>();

	public static Map<String, String> serialize(IDomainException exception) {
		return serialize(exception, true);
	}
	public static Map<String, String> serialize(IDomainException exception, boolean loggerUse) {
		Map<String, String> rMap = new HashMap<>();
		
		Map<String, Field> reflectFields = getReflectFields(exception.getClass());
		EachUtilx.forEach(reflectFields, (n, f) -> {

			if(!loggerUse)
				// 忽略特定两个字段，他们会被显式地设置到发送的message对象，没必要多做一次序列化
				if("id".equals(n) || "timestamp".equals(n))
					return;
			
			Object fValue;
			try {
				fValue = f.get(exception);
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
			rMap.put(n, sValue(f.getType(), fValue));
			
		});
		
		return rMap;
	}
	
	public static IDomainException deserialize(Map<String, String> pMap, Class<? extends IDomainException> exceptionClazz) {
		
		return (IDomainException )(MapUtilx
				.getOrAdd(builderMap, exceptionClazz, () -> new EJokerInstanceBuilder(exceptionClazz))
				.doCreate(e -> {
			Map<String, Field> reflectFields = getReflectFields(exceptionClazz);
			EachUtilx.forEach(reflectFields, (n, f) -> {
				

				// 忽略特定两个字段
				if("id".equals(n) || "timestamp".equals(n))
					return;
				
				Object dValue = dValue(f.getType(), pMap.get(n));
				try {
					f.set(e, dValue);
				} catch (IllegalArgumentException | IllegalAccessException ex) {
					throw new RuntimeException(ex.getMessage(), ex);
				}
				
			});
			
		}));
		
	}
	
	public static Map<String, Field> getReflectFields(Class<? extends IDomainException> exceptionClazz)  {
		return MapUtilx.getOrAdd(reflectMap, exceptionClazz, () -> {
			
			Map<String, Field> rMap = new HashMap<>();
			
			for(Class<?> current = exceptionClazz;
					!RuntimeException.class.equals(current);
					current = current.getSuperclass()) {
				
				EachUtilx.forEach(current.getDeclaredFields(), (field) -> {
					
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();
					
					// 略过Java规范中的一些非业务字段
					if("serialVersionUID".equals(fieldName))
						return;
					
					// 跳过有final和static修饰的字段
					if(Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
						return;
					
					// 略过PersistentIgnore注解的字段
					if(field.isAnnotationPresent(PersistentIgnore.class))
						return;
					
					// 如果有不能直接序列化字段且不是枚举类型，同时通过了上面3个判断，则报错
					if(!SerializableCheckerUtil.isDirectSerializableType(fieldType) && !fieldType.isEnum())
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
		} else if(type.isEnum()) {
			return ((Enum<?> )value).name();
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
		} else if (type.isEnum()) {
			return revertIntoEnumType(type, tValue);
		}
		throw new RuntimeException(String.format("Unsupport type[%s] on PublishableException!!!", type.getName()));
	}

	//// 以下代码引自 com.jiefzz.ejoker.z.common.utils.relationship.RelationshipTreeRevertUtil #219
	/**
	 * 还原枚举类型，通过枚举的表现字符值
	 */
	private static <TEnum> TEnum revertIntoEnumType(Class<TEnum> enumType, String represent){
		Object value = null;
		if(enumType.isEnum()) {
			Map<String, Enum<?>> eInfoMap;
			if(eMapItemPlaceHolder.equals(eInfoMap = eMap.getOrDefault(enumType, eMapItemPlaceHolder))) {
				eInfoMap = new HashMap<>();
				TEnum[] enumConstants = enumType.getEnumConstants();
				for(TEnum obj:enumConstants) {
					eInfoMap.put(obj.toString(), (Enum<?> )obj);
				}
				eMap.putIfAbsent((Class<Enum<?>> )enumType, eInfoMap);
			};
			value = eInfoMap.get(represent);
		} else {
			throw new RuntimeException(String.format("[%s] is not a Enum type!!!", enumType.getName()));
		}
		if(null == value) {
			throw new RuntimeException(String.format("[%s] has not such a value[%s]!!!", enumType.getName(), represent));
		}
		return (TEnum )value;
	}
	private static Map<Class<Enum<?>>, Map<String, Enum<?>>> eMap = new HashMap<>();
	private final static Map<String, Enum<?>> eMapItemPlaceHolder = new HashMap<>();
	
	private final static Map<Class<? extends IDomainException>, EJokerInstanceBuilder> builderMap = new HashMap<>();
}
