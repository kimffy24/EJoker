package com.jiefzz.ejoker.queue.skeleton;

import com.jiefzz.ejoker.queue.skeleton.clients.consumer.IConsumer;
import com.jiefzz.ejoker.z.common.service.IWorkerService;

public interface IQueueComsumerWokerService extends IWorkerService {
	
	public IQueueComsumerWokerService subscribe(String topic);

	public IConsumer getConsumer();
	public IQueueComsumerWokerService useConsumer(IConsumer consumer);
    
}
