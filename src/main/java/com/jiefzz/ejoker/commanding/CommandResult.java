package com.jiefzz.ejoker.commanding;

public class CommandResult {

	public CommandStatus status;
	public String commandId;
	public String aggregateRootId;
	public String result;
	public String resultType;
	
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
		this(status, commandId, aggregateRootId, null);
	}
	
	public enum CommandStatus {
        None,
        Success,
        NothingChanged,
        Failed
    }
}
