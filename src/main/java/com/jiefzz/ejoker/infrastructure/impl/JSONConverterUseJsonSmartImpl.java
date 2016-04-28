package com.jiefzz.ejoker.infrastructure.impl;

import com.jiefzz.ejoker.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.utils.RelationshipTreeUtil;
import com.jiefzz.ejoker.utils.RelationshipTreeUtilCallbackInterface;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;

@EService
public class JSONConverterUseJsonSmartImpl implements IJSONConverter {

	private RelationshipTreeUtil<JSONObject, JSONArray> relationshipTreeUtil = new RelationshipTreeUtil<JSONObject, JSONArray>(new BuilderToolSet());
	
	@Override
	public <T> String convert(T object) {
		JSONObject result;
		try {
			result = relationshipTreeUtil.getTreeStructureMap(object);
		} catch (Exception e) {
			throw new InfrastructureRuntimeException("", e);
		}
		return JSONValue.toJSONString(result, JSONStyle.NO_COMPRESS);
	}

	@Override
	public <T> T revert(String jsonString, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> void contain(String jsonString, T container) {
		throw new InfrastructureRuntimeException("Umimplemented!!!");
	}

	/**
	 * 提供树形结构转化时需要的客户端方法
	 * @author JiefzzLon
	 *
	 */
	public class BuilderToolSet implements RelationshipTreeUtilCallbackInterface<JSONObject, JSONArray>{
		@Override
		public JSONObject createNode() throws Exception {
			return new JSONObject();
		}
		
		@Override
		public JSONArray createValueSet() throws Exception {
			return new JSONArray();
		}
		
		@Override
		public boolean isHas(JSONObject targetNode, String key) throws Exception {
			return targetNode.containsKey(key);
		}

		@Override
		public void addToValueSet(JSONArray valueSet, Object child) throws Exception {
			valueSet.add(child);
		}

		@Override
		public void addToKeyValueSet(JSONObject keyValueSet, Object child, String key) throws Exception {
			keyValueSet.put(key, child);
		}
		
		@Override
		public void merge(JSONObject targetNode, JSONObject tempNode) throws Exception {
			targetNode.putAll(tempNode);
		}

		@Override
		public Object getOne(JSONObject targetNode, String key) throws Exception {
			return targetNode.get(key);
		}
	}

}
