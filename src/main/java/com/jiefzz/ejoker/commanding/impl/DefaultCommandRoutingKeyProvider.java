package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandRoutingKeyProvider;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultCommandRoutingKeyProvider implements ICommandRoutingKeyProvider {

	private final static String DEFAULT_ROUTING_KEY_PREFIX = "command.";
	
	@Override
	public String getRoutingKey(ICommand command) {
		if(null==command.getAggregateRootId() || "".equals(command.getAggregateRootId()))
			return DEFAULT_ROUTING_KEY_PREFIX +command.getAggregateRootId();
		return DEFAULT_ROUTING_KEY_PREFIX +command.getId();
	}

}
