package com.jiefzz.ejoker.queue.skeleton;

import com.jiefzz.ejoker.queue.skeleton.clients.producer.IProducer;
import com.jiefzz.ejoker.z.common.service.IWorkerService;

public interface IQueueProducerWokerService extends IWorkerService {
	

	public IProducer getProducer();
	public IQueueProducerWokerService useProducer(IProducer producer);
	
}
