package com.jiefzz.ejoker.z.queue;

public interface IQueueWokerService {
	
	public IQueueWokerService start();
	public IQueueWokerService subscribe(String topic);
    public IQueueWokerService shutdown();
    
}
