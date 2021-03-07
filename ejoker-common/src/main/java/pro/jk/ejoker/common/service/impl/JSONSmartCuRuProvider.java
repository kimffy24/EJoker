package pro.jk.ejoker.common.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pro.jk.ejoker.common.utils.relationship.IRelationshipScalpel;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeUtil;
import pro.jk.ejoker.common.utils.relationship.SpecialTypeCodec;
import pro.jk.ejoker.common.utils.relationship.SpecialTypeCodecStore;

public class JSONSmartCuRuProvider {

	private SpecialTypeCodecStore<String> specialTypeHandler;
	
	public final RelationshipTreeUtil<JSONObject, JSONArray> relationshipTreeUtil;

	public final RelationshipTreeRevertUtil<JSONObject, JSONArray> revertRelationshipTreeUitl;
	
	@SuppressWarnings("unchecked")
	private JSONSmartCuRuProvider() {
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
						return String.valueOf(target);
					}

					@Override
					public Character decode(String source) {
						if(null == source || source.isEmpty())
							return (char )0;
						return source.charAt(0);
					}
					
				})
				.append(Character.class, new SpecialTypeCodec<Character, String>(){

					@Override
					public String encode(Character target) {
						return String.valueOf(target);
					}

					@Override
					public Character decode(String source) {
						if(null == source || source.isEmpty())
							return null;
						return source.charAt(0);
					}
					
				})
				;
		
		IRelationshipScalpel<JSONObject, JSONArray> eval = new IRelationshipScalpel<JSONObject, JSONArray>() {
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

			@Override
			public Object getFromKeyValeSet(JSONObject targetNode, Object key) {
				return targetNode.get(key);
			}
			@Override
			public boolean hasKey(JSONObject source, Object key) {
				return source.containsKey(key);
			}

			@Override
			public Object getValue(JSONArray source, int index) {
				return source.get(index);
			}

			@Override
			public int getVPSize(JSONArray source) {
				return source.size();
			}

			@SuppressWarnings("rawtypes")
			@Override
			public Set getKeySet(JSONObject source) {
				return source.keySet();
			}

		};
		
		relationshipTreeUtil = new RelationshipTreeUtil<JSONObject, JSONArray>(eval, specialTypeHandler);
		
		revertRelationshipTreeUitl = new RelationshipTreeRevertUtil<JSONObject, JSONArray>(eval, specialTypeHandler);
	}
	
	private final static JSONSmartCuRuProvider instance;
	
	static {
		instance = new JSONSmartCuRuProvider();
	}
	
	public final static JSONSmartCuRuProvider getInstance() {
		return instance;
	}
	
}
