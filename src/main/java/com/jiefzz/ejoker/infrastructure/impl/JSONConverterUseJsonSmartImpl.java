package com.jiefzz.ejoker.infrastructure.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.utils.relationship.RelationshipTreeUtil;
import com.jiefzz.ejoker.utils.relationship.RelationshipTreeUtilCallbackInterface;
import com.jiefzz.ejoker.utils.relationship.RevertRelationshipTreeDisassemblyInterface;
import com.jiefzz.ejoker.utils.relationship.RevertRelationshipTreeUitl;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

@EService
public class JSONConverterUseJsonSmartImpl implements IJSONConverter {

	private final static Logger logger = LoggerFactory.getLogger(JSONConverterUseJsonSmartImpl.class);

	private RelationshipTreeUtil<JSONObject, JSONArray> relationshipTreeUtil =
			new RelationshipTreeUtil<JSONObject, JSONArray>(new RelationshipTreeUtilCallbackInterface<JSONObject, JSONArray>() {
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
			});

	private static final RevertRelationshipTreeUitl<JSONObject, JSONArray> revertRelationshipTreeUitl = 
			new RevertRelationshipTreeUitl<JSONObject, JSONArray>(new RevertRelationshipTreeDisassemblyInterface<JSONObject, JSONArray>() {

				@Override
				public JSONObject getChildKVP(JSONObject source, String key) {
					return (JSONObject )source.get(key);
				}

				@Override
				public JSONObject getChildKVP(JSONArray source, int index) {
					return (JSONObject )source.get(index);
				}

				@Override
				public JSONArray getChildVP(JSONObject source, String key) {
					return (JSONArray )source.get(key);
				}

				@Override
				public JSONArray getChildVP(JSONArray source, int index) {
					return (JSONArray )source.get(index);
				}

				@Override
				public Object getValue(JSONObject source, String key) {
					return source.get(key);
				}

				@Override
				public Object getValue(JSONArray source, int index) {
					return source.get(index);
				}

				@Override
				public int getVPSize(JSONArray source) {
					return source.size();
				}

				@Override
				public Map convertNodeAsMap(JSONObject source) {
					return (Map )source;
				}

				@Override
				public Set convertNodeAsSet(JSONArray source) {
					Set resultSet = new HashSet();
					resultSet.addAll(source);
					return resultSet;
				}

				@Override
				public List convertNodeAsList(JSONArray source) {
					return (List )source;
				}
			});
	
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
		try {
			return revertRelationshipTreeUitl.revert(clazz, (JSONObject )JSONValue.parseStrict(jsonString));
		} catch (ParseException e) {
			throw new InfrastructureRuntimeException("revert JsonObject failed!!!", e);
		}
	}

}
