package pro.jiefzz.ejoker.common.utils.relationship;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pro.jk.ejoker.common.utils.relationship.IRelationshipScalpel;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeUtil;

public class StandRelationRoot {
	
	private final static IRelationshipScalpel<Map<String, Object>, List<Object>> eval = new IRelationshipScalpel<Map<String, Object>, List<Object>>() {

		@Override
		public Map<String, Object> createKeyValueSet() {
			return new HashMap<>();
		}

		@Override
		public List<Object> createValueSet() {
			return new LinkedList<>();
		}

		@Override
		public boolean isHas(Map<String, Object> targetNode, Object key) {
			return targetNode.containsKey(key);
		}

		@Override
		public void addToValueSet(List<Object> valueSet, Object child) {
			// TODO Auto-generated method stub
			valueSet.add(child);
		}

		@Override
		public void addToKeyValueSet(Map<String, Object> keyValueSet, Object child, String key) {
			keyValueSet.put(key, child);
		}

		@Override
		public Object getFromKeyValeSet(Map<String, Object> targetNode, Object key) {
			return targetNode.get(key);
		}
		
		@Override
		public boolean hasKey(Map<String, Object> source, Object key) {
			return source.containsKey(key);
		}

		@Override
		public Object getValue(List<Object> source, int index) {
			return source.get(index);
		}

		@Override
		public int getVPSize(List<Object> source) {
			return source.size();
		}

		@Override
		public Set<String> getKeySet(Map<String, Object> source) {
			return source.keySet();
		}
	};

	protected final static RelationshipTreeUtil<Map<String, Object>, List<Object>> cu = new RelationshipTreeUtil<>(eval);

	protected final static RelationshipTreeRevertUtil<Map<String, Object>, List<Object>> ru = new RelationshipTreeRevertUtil<>(eval);;
}
