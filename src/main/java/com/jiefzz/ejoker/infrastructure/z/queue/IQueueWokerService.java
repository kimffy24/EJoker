package com.jiefzz.ejoker.infrastructure.z.queue;

public interface IQueueWokerService {
	
	public IQueueWokerService start();
	public IQueueWokerService subscribe(String topic);
    public IQueueWokerService shutdown();
    
}
