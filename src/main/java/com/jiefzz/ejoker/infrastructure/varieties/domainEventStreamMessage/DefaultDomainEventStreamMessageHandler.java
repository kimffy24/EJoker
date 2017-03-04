package com.jiefzz.ejoker.infrastructure.varieties.domainEventStreamMessage;

import java.util.List;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.infrastructure.AbstractMessageHandler;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.varieties.DomainEventStreamMessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

@DomainEventStreamMessageHandler
@EService
public class DefaultDomainEventStreamMessageHandler extends AbstractMessageHandler {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	public Future<AsyncTaskResultBase> handleAsync(DomainEventStreamMessage domainEventStreamMessage) {
		
		List<IDomainEvent<?>> events = domainEventStreamMessage.getEvents();
		
		return messageDispatcher.dispatchMessagesAsync(events);
	}

}
