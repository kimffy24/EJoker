package pro.jk.ejoker.queue.domainEvent;

import java.io.Serializable;

import pro.jk.ejoker.common.context.annotation.persistent.PersistentIgnore;

public class DomainEventHandledMessage implements Serializable {

	@PersistentIgnore
	private static final long serialVersionUID = -2006858053308658411L;

	private String commandId;
	
	private String aggregateRootId;
	
	private String commandResult;
	
	public String getCommandId() {
		return commandId;
	}
	
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}
	
	public String getAggregateRootId() {
		return aggregateRootId;
	}
	
	public void setAggregateRootId(String aggregateRootId) {
		this.aggregateRootId = aggregateRootId;
	}
	
	public String getCommandResult() {
		return commandResult;
	}
	
	public void setCommandResult(String commandResult) {
		this.commandResult = commandResult;
	}
}
