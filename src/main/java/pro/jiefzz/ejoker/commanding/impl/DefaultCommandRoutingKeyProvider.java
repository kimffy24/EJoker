package pro.jiefzz.ejoker.commanding.impl;

import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandRoutingKeyProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;

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
