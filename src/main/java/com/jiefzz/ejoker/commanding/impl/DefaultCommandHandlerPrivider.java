package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.CommandRuntimeException;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandHandlerPrivider;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool.HandlerReflectionMapper;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * 
 * In eNode use assembly info to find target Handler!;
 * @author jiefzz
 *
 */
@EService
public class DefaultCommandHandlerPrivider implements ICommandHandlerPrivider {

	@Override
	public ICommandHandlerProxy getHandler(Class<? extends ICommand> commandType) {
		HandlerReflectionMapper handlerReflection = CommandHandlerPool.handlerMapper.getOrDefault(commandType, null);
		if(null==handlerReflection)
			throw new CommandRuntimeException(commandType.getName() +" is no handler found for it!!!");
		return handlerReflection;
	}
}