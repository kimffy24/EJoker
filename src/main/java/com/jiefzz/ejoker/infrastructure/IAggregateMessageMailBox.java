package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IAggregateMessageMailBox<TMessage extends IAggregateMessageMailBoxMessage<TMessage, TMessageProcessResult>, TMessageProcessResult> {

	public String getAggregateRootId();
    
	public long getLastActiveTime();
    
	public boolean isRunning();
    
	public long getConsumingSequence();
    
	public long getConsumedSequence();
    
	public long getMaxMessageSequence();
    
	public long getTotalUnConsumedMessageCount();

	// C# 下可同时在接口中设置变量已经读写的可达权限，老家伙java却不行。
	/*******************/
	
    void enqueueMessage(TMessage message);
    

    default void tryRun() {
    	this.tryRun(false);
    };
    
    void tryRun(boolean exitFirst);
    
    SystemFutureWrapper<Void> run();
    
    void pause();
    
    void resume();
    
    void resetConsumingSequence(long consumingSequence);
    
    void exit();
    
    void clear();
    
    SystemFutureWrapper<Void> completeMessage(TMessage message, TMessageProcessResult result);
    
    boolean isInactive(long timeoutSeconds);
	
}
