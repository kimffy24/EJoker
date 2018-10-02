package com.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public class ProcessingCommand {

	private ProcessingCommandMailbox mailbox;
	
	private long sequence;
	
	private ICommand message;
	
	private ICommandExecuteContext commandExecuteContext;
	
    private Map<String, String> items;

    public ProcessingCommand(ICommand command, ICommandExecuteContext commandExecuteContext, Map<String, String> items) {
    	setMessage(command);
    	setCommandExecuteContext(commandExecuteContext);
    	setItems(null != items ? items : new HashMap<>());
    }

    public SystemFutureWrapper<AsyncTaskResult<Void>> complete(CommandResult commandResult) {
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
