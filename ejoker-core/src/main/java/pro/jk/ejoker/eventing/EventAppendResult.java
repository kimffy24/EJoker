package pro.jk.ejoker.eventing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventAppendResult {
	
    private Map<String, Object> successAggregateRootIdList;
    
    private Map<String, Object> duplicateEventAggregateRootIdList;
    
    private Map<String, List<String>> duplicateCommandAggregateRootIdList;

    public EventAppendResult() {
        successAggregateRootIdList = new ConcurrentHashMap<>();
        duplicateEventAggregateRootIdList = new ConcurrentHashMap<>();
        duplicateCommandAggregateRootIdList = new ConcurrentHashMap<>();
    }

    public void addSuccessAggregateRootId(String aggregateRootId) {
        if (!successAggregateRootIdList.containsKey(aggregateRootId)) {
            successAggregateRootIdList.put(aggregateRootId, 1);
        }
    }
    public void addDuplicateEventAggregateRootId(String aggregateRootId) {
        if (!duplicateEventAggregateRootIdList.containsKey(aggregateRootId)) {
            duplicateEventAggregateRootIdList.put(aggregateRootId, 1);
        }
    }
    public void addDuplicateCommandIds(String aggregateRootId, List<String> aggregateDuplicateCommandIdList) {
        if (!duplicateCommandAggregateRootIdList.containsKey(aggregateRootId)) {
        	duplicateCommandAggregateRootIdList.put(aggregateRootId, aggregateDuplicateCommandIdList);
        }
    }
    
	public Set<String> getSuccessAggregateRootIdList() {
		return successAggregateRootIdList.keySet();
	}

	public Set<String> getDuplicateEventAggregateRootIdList() {
		return duplicateEventAggregateRootIdList.keySet();
	}

	public Map<String, List<String>> getDuplicateCommandAggregateRootIdList() {
		return duplicateCommandAggregateRootIdList;
	}
	
}
