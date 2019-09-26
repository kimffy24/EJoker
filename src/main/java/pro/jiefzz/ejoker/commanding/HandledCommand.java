package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;

public class HandledCommand {

	private String commandId;
	
	private String aggregateRootId;
	
	private IApplicationMessage message;

    /**
     * Default constructor.
     */
    public HandledCommand() {
    }

    /**
     * Parameterized constructor.
     * @param commandId
     * @param aggregateRootId
     * @param message
     */
	public HandledCommand(String commandId, String aggregateRootId, IApplicationMessage message) {
		super();
		this.commandId = commandId;
		this.aggregateRootId = aggregateRootId;
		this.message = message;
	}
	
	public HandledCommand(String commandId, String aggregateRootId) {
		this(commandId, aggregateRootId, null);
	}
	public HandledCommand(String commandId) {
		this(commandId, null, null);
	}

	public String getCommandId() {
		return commandId;
	}

	public String getAggregateRootId() {
		return aggregateRootId;
	}

	public IApplicationMessage getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return String.format("[CommandId=%s, AggregateRootId=%s, Message=%s]",
            commandId,
            aggregateRootId,
            message == null ? null : String.format("[id: %s, type: %s]", message.getId(), message.getClass().getName()));
	}
    
}
