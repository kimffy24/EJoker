package com.jiefzz.ejoker.infrastructure.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.utils.relationship.IRelationshipTreeAssemblers;
import com.jiefzz.ejoker.z.common.utils.relationship.IRelationshipTreeDisassemblers;
import com.jiefzz.ejoker.z.common.utils.relationship.RelationshipTreeRevertUtil;
import com.jiefzz.ejoker.z.common.utils.relationship.RelationshipTreeUtil;
import com.jiefzz.ejoker.z.common.utils.relationship.SpecialTypeCodec;
import com.jiefzz.ejoker.z.common.utils.relationship.SpecialTypeCodecStore;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

@EService
public class JSONConverterUseJsonSmartImpl implements IJSONConverter {

	private final static Logger logger = LoggerFactory.getLogger(JSONConverterUseJsonSmartImpl.class);

	private SpecialTypeCodecStore<String> specialTypeHandler;
	
	private RelationshipTreeUtil<JSONObject, JSONArray> relationshipTreeUtil;

	private RelationshipTreeRevertUtil<JSONObject, JSONArray> revertRelationshipTreeUitl;
	
	@SuppressWarnings("unchecked")
	public JSONConverterUseJsonSmartImpl() {
		specialTypeHandler = new SpecialTypeCodecStore<String>()
				.append(BigDecimal.class, new SpecialTypeCodec<BigDecimal, String>(){

					@Override
					public String encode(BigDecimal target) {
						return target.toPlainString();
					}

					@Override
					public BigDecimal decode(String source) {
						return new BigDecimal(source);
					}
					
				})
				.append(BigInteger.class, new SpecialTypeCodec<BigInteger, String>(){

					@Override
					public String encode(BigInteger target) {
						return target.toString();
					}

					@Override
					public BigInteger decode(String source) {
						return new BigInteger(source);
					}
					
				})
				.append(char.class, new SpecialTypeCodec<Character, String>(){

					@Override
					public String encode(Character target) {
						return "" + (int )target.charValue();
					}

					@Override
					public Character decode(String source) {
						return (char )Integer.parseInt(source);
					}
					
				})
				.append(Character.class, new SpecialTypeCodec<Character, String>(){

					@Override
					public String encode(Character target) {
						return "" + (int )target.charValue();
					}

					@Override
					public Character decode(String source) {
						return (char )Integer.parseInt(source);
					}
					
				})
				;
		
		relationshipTreeUtil = new RelationshipTreeUtil<JSONObject, JSONArray>(new IRelationshipTreeAssemblers<JSONObject, JSONArray>() {
					@Override
					public JSONObject createKeyValueSet() {
						return new JSONObject();
					}

					@Override
					public JSONArray createValueSet() {
						return new JSONArray();
					}

					@Override
					public boolean isHas(JSONObject targetNode, Object key) {
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

				}, specialTypeHandler);
		
		revertRelationshipTreeUitl = new RelationshipTreeRevertUtil<JSONObject, JSONArray>(new IRelationshipTreeDisassemblers<JSONObject, JSONArray>() {

					@Override
					public Object getValue(JSONObject source, Object key) {
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
					public Set getKeySet(JSONObject source) {
						return source.keySet();
					}
					
				}, specialTypeHandler);
	}
	
	@Override
	public <T> String convert(T object) {
		JSONObject result;
		try {
			result = relationshipTreeUtil.getTreeStructure(object);
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
			return revertRelationshipTreeUitl.revert((JSONObject )JSONValue.parseStrict(jsonString), clazz);
		} catch (ParseException e) {
			throw new InfrastructureRuntimeException("revert JsonObject failed!!!", e);
		}
	}

}
