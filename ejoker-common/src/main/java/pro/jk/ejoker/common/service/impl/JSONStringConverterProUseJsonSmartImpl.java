package pro.jk.ejoker.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeUtil;

@EService
public class JSONStringConverterProUseJsonSmartImpl implements IJSONStringConverterPro {

	private final static Logger logger = LoggerFactory.getLogger(JSONStringConverterProUseJsonSmartImpl.class);

	private RelationshipTreeUtil<JSONObject, JSONArray> cu = JSONSmartCuRuProvider.getInstance().relationshipTreeUtil;
	
	private RelationshipTreeRevertUtil<JSONObject, JSONArray> ru = JSONSmartCuRuProvider.getInstance().revertRelationshipTreeUitl;

	@Override
	public String convert(Object object) {
		JSONObject treeStructure = cu.getTreeStructure(object);
		return null == treeStructure ? "" : treeStructure.toJSONString();
	}

	@Override
	public <T> String convert(Object object, TypeRefer<T> tr) {
		if(null == object)
			return "";
		// JSONObject 和  JSONArray 的 toString 方法都被重写为 toJSONString 了
		return cu.getTreeStructure(object, tr).toString();
	}

	@Override
	public <T> T revert(String jsonString, TypeRefer<T> tr) {
		if(StringUtilx.isNullOrWhiteSpace(jsonString))
			return null;
		Object parseStrict;
		try {
			parseStrict = JSONValue.parseStrict(jsonString);
		} catch (ParseException e) {
			logger.error("revert JsonObject failed!!!", e);
			throw new RuntimeException("revert JsonObject failed!!!", e);
		}
		return ru.revert(parseStrict, tr);
	}

}
