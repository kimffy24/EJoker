package pro.jk.ejoker.queue.command;

public class CommandMessage {

	protected String commandData;
	
	protected String replyAddress;
	
	public CommandMessage(){
	}
	public CommandMessage(String commandData, String replyAddress) {
		this.commandData = commandData;
		this.replyAddress = replyAddress;
	}
	
	public String getCommandData() {
		return commandData;
	}
	
	public void setCommandData(String commandData) {
		this.commandData = commandData;
	}
	
	public String getReplyAddress() {
		return replyAddress;
	}
	
	public void setReplyAddress(String replyAddress) {
		this.replyAddress = replyAddress;
	}
	
}
