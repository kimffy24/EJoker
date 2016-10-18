package com.jiefzz.ejoker.utils.relationship;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RevertRelationshipTreeDisassemblyInterface<KVP, VP> {
	
	public KVP getChildKVP(KVP source, String key);
	public KVP getChildKVP(VP source, int index);
	
	public VP getChildVP(KVP source, String key);
	public VP getChildVP(VP source, int index);
	
	public Object getValue(KVP source, String key);
	public Object getValue(VP source, int index);

	public int getVPSize(VP source);
	
	public Map convertNodeAsMap(KVP source);
	public Set convertNodeAsSet(VP source);
	public List convertNodeAsList(VP source);
	
}
