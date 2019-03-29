package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandRoutingKeyProvider;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

@EService
public class DefaultCommandRoutingKeyProvider implements ICommandRoutingKeyProvider {

	@Override
	public String getRoutingKey(ICommand command) {
		String aggregateRootId = command.getAggregateRootId();
		if(!StringHelper.isNullOrWhiteSpace(aggregateRootId))
			return aggregateRootId;
		return command.getId();
	}

}
