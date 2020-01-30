package pro.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class ProcessingCommand {

	private ProcessingCommandMailbox mailBox;
	
	private long sequence;
	
	private ICommand message;
	
	private ICommandExecuteContext commandExecuteContext;
	
    private Map<String, String> items;
    
    private boolean duplicated = false;

    public ProcessingCommand(ICommand command, ICommandExecuteContext commandExecuteContext, Map<String, String> items) {
    	this.message = command;
    	this.commandExecuteContext = commandExecuteContext;
    	this.items = (null != items ? items : new HashMap<>());
    }

    public Future<Void> finishAsync(CommandResult commandResult) {
    	return commandExecuteContext.onCommandExecutedAsync(commandResult);
    }
    
    /* =========================== */


	public long getSequence() {
		return sequence;
	}

	public void setSequence(long sequence) {
		this.sequence = sequence;
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

	public ProcessingCommandMailbox getMailBox() {
		return mailBox;
	}

	public void setMailBox(ProcessingCommandMailbox mailbox) {
		this.mailBox = mailbox;
	}

	public boolean isDuplicated() {
		return duplicated;
	}

	public void setDuplicated(boolean duplicated) {
		this.duplicated = duplicated;
	}
	
}
