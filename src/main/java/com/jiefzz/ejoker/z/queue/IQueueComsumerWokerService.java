package com.jiefzz.ejoker.z.queue;

import com.jiefzz.ejoker.z.common.service.IWorkerService;

public interface IQueueComsumerWokerService extends IWorkerService {
	
	public IQueueComsumerWokerService subscribe(String topic);
    
}
