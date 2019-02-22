package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandHandlerProvider;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * 
 * In eNode use assembly info to find target Handler!;
 * @author jiefzz
 *
 */
@EService
public class DefaultCommandHandlerProvider implements ICommandHandlerProvider {
	
	@Dependence
	CommandHandlerPool commandHandlerPool;

	@Override
	public ICommandHandlerProxy getHandler(Class<? extends ICommand> commandType) {
		return commandHandlerPool.fetchCommandHandler(commandType);
	}
}