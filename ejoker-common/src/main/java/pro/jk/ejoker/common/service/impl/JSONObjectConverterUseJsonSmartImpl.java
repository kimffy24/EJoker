package pro.jk.ejoker.common.service.impl;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONObjectConverter;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeUtil;

@EService
public class JSONObjectConverterUseJsonSmartImpl implements IJSONObjectConverter {

	private RelationshipTreeUtil<JSONObject, JSONArray> relationshipTreeUtil = JSONSmartCuRuProvider.getInstance().relationshipTreeUtil;

	private RelationshipTreeRevertUtil<JSONObject, JSONArray> revertRelationshipTreeUitl = JSONSmartCuRuProvider.getInstance().revertRelationshipTreeUitl;
	
	@Override
	public <T> JSONObject convert(T object) {
		return relationshipTreeUtil.getTreeStructure(object);
	}

	@Override
	public <T> T revert(JSONObject jsonObject, Class<T> clazz) {
		return revertRelationshipTreeUitl.revert(jsonObject, clazz);
	}

}
