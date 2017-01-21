package com.jiefzz.ejoker.commanding;

public interface IProcessingCommandScheduler {

	/**
	 * try to fork a new thread to handle the message on mailbox.
	 * @param mailbox
	 */
	public void scheduleMailbox(ProcessingCommandMailbox mailbox);
	
	/**
	 * feedback that a thread is end.
	 */
	public void completeOneSchedule();
	
}
