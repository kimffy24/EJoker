package com.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public class ProcessingCommand {

	private ProcessingCommandMailbox mailbox;
	
	private long sequence;
	
	private ICommand message;
	
	private ICommandExecuteContext commandExecuteContext;
	
    private Map<String, String> items;

    public ProcessingCommand(ICommand command, ICommandExecuteContext commandExecuteContext, Map<String, String> items) {
    	this.message = command;
    	this.commandExecuteContext = commandExecuteContext;
    	this.items = (null != items ? items : new HashMap<>());
    }

    public void complete(CommandResult commandResult) {
    	commandExecuteContext.onCommandExecuted(commandResult);
    }

    public SystemFutureWrapper<Void> completeAsync(CommandResult commandResult) {
    	return commandExecuteContext.onCommandExecutedAsync(commandResult);
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

}
