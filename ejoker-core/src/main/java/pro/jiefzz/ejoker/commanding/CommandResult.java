package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.common.system.helper.StringHelper;

public class CommandResult {

	private CommandStatus status;
	private String commandId;
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
		return StringHelper.fill("\\{commandId: {}, status: {}, aggregateRootId: {}, result: {}, resultType: {}\\}",
				commandId,
				status.toString(),
				aggregateRootId,
				result,
				resultType
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
