package com.jiefzz.ejoker.infrastructure.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.utils.ParameterizedTypeUtil;
import com.jiefzz.ejoker.utils.RelationshipTreeUtil;
import com.jiefzz.ejoker.utils.RelationshipTreeUtilCallbackInterface;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentTop;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;

@EService
public class JSONConverterUseJsonSmartImpl implements IJSONConverter {

	private final static Logger logger = LoggerFactory.getLogger(JSONConverterUseJsonSmartImpl.class);

	private final static Map<String, Field> defaultEmptyInfo = new HashMap<String, Field>();
	private final Map<Class<?>, Map<String, Field>> clazzRefectionInfo = new ConcurrentHashMap<Class<?>, Map<String, Field>>();
	
	private RelationshipTreeUtil<JSONObject, JSONArray> relationshipTreeUtil = new RelationshipTreeUtil<JSONObject, JSONArray>(new BuilderToolSet());

	@Override
	public <T> String convert(T object) {
		JSONObject result;
		try {
			result = relationshipTreeUtil.getTreeStructureMap(object);
		} catch (Exception e) {
			String format = String.format("Could not convert {%s} to JsonString", object.getClass().getName());
			logger.error(format);
			throw new InfrastructureRuntimeException(format, e);
		}
		return JSONValue.toJSONString(result, JSONStyle.NO_COMPRESS);
	}

	@Override
	public <T> T revert(String jsonString, Class<T> clazz) {
		T newInstance=null;
		try {
			newInstance = clazz.newInstance();
		} catch (Exception e) {
			String format = String.format("Could not revert into [%s] with JsonString: \"%s\"", clazz.getName(), jsonString);
			logger.error(format);
			throw new InfrastructureRuntimeException(format, e);
		}
		Object parse = JSONValue.parse(jsonString);
		if(parse instanceof JSONObject) {
			Map<String, Field> analyzeClazzInfo = analyzeClazzInfo(clazz);
			Set<Entry<String, Object>> entrySet = ((JSONObject) parse).entrySet();
			for(Entry<String, Object> entry : entrySet) {
				String key = entry.getKey();
				Field field = analyzeClazzInfo.get(key);
				Map<String, Field> analyzeClazzInfoChild = analyzeClazzInfo(field.getType());
				Object fieldValue = null;
				if(analyzeClazzInfoChild!=null) {
					// It means this field is not Collection or Base type.
					String asString = ((JSONObject) parse).getAsString(key);
					logger.debug("Recursion invoke with FieldType: [{}], JsonString: [{}]", field.getType().getName(), asString);
					fieldValue = revert(asString, field.getType());
				} else 
					fieldValue = entry.getValue();

				field.setAccessible(true);
				try {
					field.set(newInstance, fieldValue);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return newInstance;
		}
		logger.error("Unsupport revert Type {}", clazz);
		return null;
	}

	@Deprecated
	@Override
	public <T> void contain(String jsonString, T container) {
		throw new InfrastructureRuntimeException("Umimplemented!!!");
	}
	
	private <T> Map<String, Field> analyzeClazzInfo(final Class<T> clazz){

		if(ParameterizedTypeUtil.hasSublevel(clazz) || ParameterizedTypeUtil.isDirectSerializableType(clazz))
			return null;
		
		Map<String, Field> analyzeResult = clazzRefectionInfo.getOrDefault(clazz, defaultEmptyInfo);
		if(analyzeResult!=defaultEmptyInfo) return analyzeResult;
		analyzeResult = new HashMap<String, Field>();
		for ( Class<?> claxx = clazz; claxx != Object.class || claxx.isAnnotationPresent(PersistentTop.class); claxx = claxx.getSuperclass() ) {
			Field[] fields = claxx.getDeclaredFields();
			for ( Field field : fields ) {
				analyzeResult.putIfAbsent(field.getName(), field);
				analyzeClazzInfo(field.getType());
			}
		}
		clazzRefectionInfo.putIfAbsent(clazz, analyzeResult);
		return analyzeResult;
	}

	/**
	 * 提供树形结构转化时需要的客户端方法
	 * @author JiefzzLon
	 *
	 */
	public class BuilderToolSet implements RelationshipTreeUtilCallbackInterface<JSONObject, JSONArray>{
		@Override
		public JSONObject createNode() {
			return new JSONObject();
		}

		@Override
		public JSONArray createValueSet() {
			return new JSONArray();
		}

		@Override
		public boolean isHas(JSONObject targetNode, String key) {
			return targetNode.containsKey(key);
		}

		@Override
		public void addToValueSet(JSONArray valueSet, Object child) {
			valueSet.add(child);
		}

		@Override
		public void addToKeyValueSet(JSONObject keyValueSet, Object child, String key) {
			keyValueSet.put(key, child);
		}

		@Override
		public void merge(JSONObject targetNode, JSONObject tempNode) {
			targetNode.putAll(tempNode);
		}

		@Override
		public Object getOne(JSONObject targetNode, String key) {
			return targetNode.get(key);
		}
	}

}
