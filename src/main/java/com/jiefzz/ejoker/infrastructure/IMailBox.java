package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IMailBox<TMessage extends IMailBoxMessage<TMessage, TMessageProcessResult>, TMessageProcessResult> {

	public String getRoutingKey();
    
	public long getLastActiveTime();
    
	public boolean isRunning();
	
	public boolean isPaused();
    
	public long getConsumingSequence();
    
	public long getConsumedSequence();
    
	public long getMaxMessageSequence();
    
	public long getTotalUnConsumedMessageCount();

	// C# 下可同时在接口中设置变量以及读写的可达权限，老家伙java却不行。
	/*******************/
	
    void enqueueMessage(TMessage message);
    
    void tryRun();
    
    void completeRun();
    
    /**
     * 使用pauseOnly() 和 acquireOnProcessing() 组合的解析在pause实现的方法写了注释。
     */
	@Deprecated
    void pause();
	
	void pauseOnly();
	
	void acquireOnProcessing();
    
    void resume();
    
    void resetConsumingSequence(long consumingSequence);
    
    void clear();
    
    SystemFutureWrapper<Void> completeMessage(TMessage message, TMessageProcessResult result);
    
    boolean isInactive(long timeoutSeconds);
	
}
