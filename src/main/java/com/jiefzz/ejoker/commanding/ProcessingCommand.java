package com.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;

public class ProcessingCommand {

	private ProcessingCommandMailbox mailbox; // { get; set; }
	private long sequence; //{ get; set; }
	private ICommand message; //{ get; private set; }
	private ICommandExecuteContext commandExecuteContext; // { get; private set; }
    private Map<String, String> items; // { get; private set; }

    public ProcessingCommand(ICommand command, ICommandExecuteContext commandExecuteContext, Map<String, String> items) {
    	setMessage(command);
    	setCommandExecuteContext(commandExecuteContext);
    	setItems(items!=null ? items : new HashMap<String, String>());
    }

    public void complete(CommandResult commandResult) {
    	commandExecuteContext.onCommandExecuted(commandResult);
    }
    
    /* =========================== */

	public ProcessingCommandMailbox getMailbox() {
		return mailbox;
	}

	public long getSequence() {
		return sequence;
	}

	public ICommand getMessage() {
		return message;
	}

	public ICommandExecuteContext getCommandExecuteContext() {
		return commandExecuteContext;
	}

	public Map<String, String> getItems() {
		return items;
	}

	public void setMailbox(ProcessingCommandMailbox mailbox) {
		this.mailbox = mailbox;
	}

	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	private void setMessage(ICommand message) {
		this.message = message;
	}

	private void setCommandExecuteContext(ICommandExecuteContext commandExecuteContext) {
		this.commandExecuteContext = commandExecuteContext;
	}

	private void setItems(Map<String, String> items) {
		this.items = items;
	}

}
