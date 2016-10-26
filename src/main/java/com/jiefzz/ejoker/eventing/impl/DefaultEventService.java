package com.jiefzz.ejoker.eventing.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventCommittingConetxt;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultEventService implements IEventService {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultEventService.class);
	
	@Override
	public void setProcessingCommandHandler(IProcessingCommandHandler processingCommandHandler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void commitDomainEventAsync(EventCommittingConetxt context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream) {
		// TODO Auto-generated method stub

	}

}
