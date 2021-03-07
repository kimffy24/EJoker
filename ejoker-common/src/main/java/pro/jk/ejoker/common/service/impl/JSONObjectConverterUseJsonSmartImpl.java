package pro.jk.ejoker.common.service.impl;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONObjectConverter;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeUtil;

@EService
public class JSONObjectConverterUseJsonSmartImpl implements IJSONObjectConverter {

	private RelationshipTreeUtil<JSONObject, JSONArray> cu = JSONSmartCuRuProvider.getInstance().relationshipTreeUtil;

	private RelationshipTreeRevertUtil<JSONObject, JSONArray> ru = JSONSmartCuRuProvider.getInstance().revertRelationshipTreeUitl;

	@Override
	public <T> JSONObject convert(Object object, TypeRefer<T> tr) {
		if(null == object)
			return new JSONObject();
		// maybe is jsonObject or maybe is jsonArray
		Object treeStructure = cu.getTreeStructure(object, tr);
		if(treeStructure instanceof JSONObject)
			return (JSONObject )treeStructure;
		throw new RuntimeException("Revert target is not a instance of JSONObject, Maybe you should use converCollection()");
	}

	@Override
	public <T> JSONArray converCollection(Object object, TypeRefer<T> tr) {
		if(null == object)
			return new JSONArray();
		// maybe is jsonObject or maybe is jsonArray
		Object treeStructure = cu.getTreeStructure(object, tr);
		if(treeStructure instanceof JSONObject)
			return (JSONArray )treeStructure;
		throw new RuntimeException("Revert target is not a instance of JSONArray, Maybe you should use convert()");
	}

	@Override
	public <T> T revert(JSONObject json, TypeRefer<T> tr) {
		return ru.revert(json, tr);
	}

}
