package pro.jiefzz.ejoker.utils.domainExceptionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import pro.jiefzz.ejoker.domain.domainException.IDomainException;
import pro.jiefzz.ejoker.z.context.annotation.persistent.PersistentIgnore;
import pro.jiefzz.ejoker.z.context.dev2.EJokerInstanceBuilder;
import pro.jiefzz.ejoker.z.system.helper.ForEachHelper;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.utils.ParameterizedTypeUtil;

public final class DomainExceptionCodecHelper {
	
	private static Map<Class<? extends IDomainException>, Map<String, Field>> reflectMap= new HashMap<>();

	public static Map<String, String> serialize(IDomainException exception) {
		Map<String, String> rMap = new HashMap<>();
		
		Map<String, Field> reflectFields = getReflectFields(exception.getClass());
		ForEachHelper.processForEach(reflectFields, (n, f) -> {

			Object fValue;
			try {
				fValue = f.get(exception);
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
			rMap.put(n, sValue(f.getType(), fValue));
			
		});

		// fixed #190929 针对 IMessage 增加的items
		JSONObject itemsDict = new JSONObject();
		Set<Entry<String, String>> entrySet = exception.getItems().entrySet();
		for(Entry<String, String> entry : entrySet) {
			itemsDict.appendField(entry.getKey(), entry.getValue());
		}
		rMap.put("items", itemsDict.toJSONString());
		
		return rMap;
	}
	
	public static IDomainException deserialize(Map<String, String> pMap, Class<? extends IDomainException> exceptionClazz) {
		
		return (IDomainException )(MapHelper
				.getOrAdd(builderMap, exceptionClazz, () -> new EJokerInstanceBuilder(exceptionClazz))
				.doCreate(e -> {
			Map<String, Field> reflectFields = getReflectFields(exceptionClazz);
			ForEachHelper.processForEach(reflectFields, (n, f) -> {
				
				Object dValue = dValue(f.getType(), pMap.get(n));
				try {
					f.set(e, dValue);
				} catch (IllegalArgumentException | IllegalAccessException ex) {
					throw new RuntimeException(ex.getMessage(), ex);
				}
				
			});

			// fixed #190929 针对 IMessage 增加的items
			JSONObject itemsDict;
			try {
				itemsDict = (JSONObject )JSONValue.parseStrict(pMap.get("items"));
			} catch (ParseException e1) {
				throw new RuntimeException(e1.getMessage(), e1);
			}
			Map<String, String> items = new HashMap<>();
			Set<Entry<String, Object>> entrySet = itemsDict.entrySet();
			for(Entry<String, Object> entry : entrySet) {
				items.put(entry.getKey(), entry.getValue().toString());
			}
			((IDomainException )e).setItems(items);
			
		}));
		
	}
	
	public static Map<String, Field> getReflectFields(Class<? extends IDomainException> exceptionClazz)  {
		return MapHelper.getOrAdd(reflectMap, exceptionClazz, () -> {
			
			Map<String, Field> rMap = new HashMap<>();
			
			for(Class<?> current = exceptionClazz;
					!RuntimeException.class.equals(current);
					current = current.getSuperclass()) {
				
				ForEachHelper.processForEach(current.getDeclaredFields(), (field) -> {
					
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();
					
					// 略过Java规范中的一些非业务字段
					if("serialVersionUID".equals(fieldName))
						return;
					
					if(Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
						return;
					
					if(field.isAnnotationPresent(PersistentIgnore.class))
						return;
					
					if(!ParameterizedTypeUtil.isDirectSerializableType(fieldType) && !fieldType.isEnum())
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
