package com.jiefzz.ejoker.queue.domainEvent;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DomainEventPublisher implements IMessagePublisher<DomainEventStreamMessage> {

	@Override
	public void publishAsync(DomainEventStreamMessage message) {
		// TODO Auto-generated method stub
		
	}

}
