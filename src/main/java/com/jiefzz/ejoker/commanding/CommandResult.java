package com.jiefzz.ejoker.commanding;

public class CommandResult {

	private String commandId;
	private CommandStatus status;
	private String aggregateRootId;
	private String result;
	private String resultType;
	
	public CommandResult() { }
	public CommandResult(CommandStatus status, String commandId, String aggregateRootId, String result, String resultType) {
		this.status = status;
		this.commandId = commandId;
		this.aggregateRootId = aggregateRootId;
		this.result = result;
		this.resultType = resultType;
	}
	public CommandResult(CommandStatus status, String commandId, String aggregateRootId, String result) {
		this(status, commandId, aggregateRootId, result, null);
	}
	public CommandResult(CommandStatus status, String commandId, String aggregateRootId) {
		this(status, commandId, aggregateRootId, null, null);
	}
	
	@Override
	public String toString() {
		return String.format(
				"[commandId=%s, status=%s, aggregateRootId=%s, result={}, resultType={}]",
				commandId, status.toString(), aggregateRootId, null==result?"null":result.toString(), null==resultType?"null":resultType.toString()
		);
	}
	
	/* *****************Getter and Setter**************** */
	
	public CommandStatus getStatus() {
		return status;
	}
	public String getCommandId() {
		return commandId;
	}
	public String getAggregateRootId() {
		return aggregateRootId;
	}
	public String getResult() {
		return result;
	}
	public String getResultType() {
		return resultType;
	}
}
