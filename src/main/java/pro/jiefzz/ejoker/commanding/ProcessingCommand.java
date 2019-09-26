package pro.jiefzz.ejoker.commanding;

import java.util.HashMap;
import java.util.Map;

import pro.jiefzz.ejoker.infrastructure.IMailBox;
import pro.jiefzz.ejoker.infrastructure.IMailBoxMessage;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public class ProcessingCommand implements IMailBoxMessage<ProcessingCommand, CommandResult> {

	private IMailBox<ProcessingCommand, CommandResult> mailBox;
	
	private long sequence;
	
	private ICommand message;
	
	private ICommandExecuteContext commandExecuteContext;
	
    private Map<String, String> items;

    public ProcessingCommand(ICommand command, ICommandExecuteContext commandExecuteContext, Map<String, String> items) {
    	this.message = command;
    	this.commandExecuteContext = commandExecuteContext;
    	this.items = (null != items ? items : new HashMap<>());
    }

    public SystemFutureWrapper<Void> completeAsync(CommandResult commandResult) {
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

	@Override
	public IMailBox<ProcessingCommand, CommandResult> getMailBox() {
		return mailBox;
	}

	@Override
	public void setMailBox(IMailBox<ProcessingCommand, CommandResult> mailbox) {
		this.mailBox = mailbox;
	}

}
