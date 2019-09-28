package pro.jiefzz.ejoker.eventing;

import java.util.ArrayList;
import java.util.List;

public class EventAppendResult {
	
    private List<String> successAggregateRootIdList;
    
    private List<String> duplicateEventAggregateRootIdList;
    
    private List<String> duplicateCommandIdList;

    public EventAppendResult() {
        successAggregateRootIdList = new ArrayList<>();
        duplicateCommandIdList = new ArrayList<>();
        duplicateEventAggregateRootIdList = new ArrayList<>();
    }

    public void addSuccessAggregateRootId(String aggregateRootId) {
        if (!successAggregateRootIdList.contains(aggregateRootId)) {
            successAggregateRootIdList.add(aggregateRootId);
        }
    }
    public void addDuplicateEventAggregateRootId(String aggregateRootId) {
        if (!duplicateEventAggregateRootIdList.contains(aggregateRootId)) {
            duplicateEventAggregateRootIdList.add(aggregateRootId);
        }
    }
    public void addDuplicateCommandId(String commandId) {
        if (!duplicateCommandIdList.contains(commandId)) {
            duplicateCommandIdList.add(commandId);
        }
    }

    
    
	public List<String> getSuccessAggregateRootIdList() {
		return successAggregateRootIdList;
	}

	public void setSuccessAggregateRootIdList(List<String> successAggregateRootIdList) {
		this.successAggregateRootIdList = successAggregateRootIdList;
	}

	public List<String> getDuplicateEventAggregateRootIdList() {
		return duplicateEventAggregateRootIdList;
	}

	public void setDuplicateEventAggregateRootIdList(List<String> duplicateEventAggregateRootIdList) {
		this.duplicateEventAggregateRootIdList = duplicateEventAggregateRootIdList;
	}

	public List<String> getDuplicateCommandIdList() {
		return duplicateCommandIdList;
	}

	public void setDuplicateCommandIdList(List<String> duplicateCommandIdList) {
		this.duplicateCommandIdList = duplicateCommandIdList;
	}

}
