package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandAsyncHandlerProvider;
import com.jiefzz.ejoker.commanding.ICommandAsyncHandlerProxy;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandAsyncHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * 
 * In eNode use assembly info to find target Handler!;
 * @author jiefzz
 *
 */
@EService
public class DefaultCommandAsyncHandlerProvider implements ICommandAsyncHandlerProvider {
	
	@Dependence
	CommandAsyncHandlerPool commandAsyncHandlerPool;

	@Override
	public ICommandAsyncHandlerProxy getHandler(Class<? extends ICommand> commandType) {
		return commandAsyncHandlerPool.fetchCommandHandler(commandType);
	}
}