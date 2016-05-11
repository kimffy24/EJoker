package com.jiefzz.ejoker.queue.command;

import java.io.Serializable;

import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.persistent.PersistentIgnore;

public class CommandMessage implements Serializable {

	@PersistentIgnore
	private static final long serialVersionUID = -9171574720716598328L;
	
	protected String commandData;
	protected String replyAddress;
	
	public CommandMessage(){}
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
