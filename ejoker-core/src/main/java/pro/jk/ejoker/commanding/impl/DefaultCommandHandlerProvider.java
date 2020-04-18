package pro.jk.ejoker.commanding.impl;

import pro.jk.ejoker.commanding.ICommand;
import pro.jk.ejoker.commanding.ICommandHandlerProvider;
import pro.jk.ejoker.commanding.ICommandHandlerProxy;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;

/**
 * 
 * In eNode use assembly info to find target Handler!;
 * @author jiefzz
 *
 */
@EService
public class DefaultCommandHandlerProvider implements ICommandHandlerProvider {
	
	@Dependence
	CommandHandlerPool commandAsyncHandlerPool;

	@Override
	public ICommandHandlerProxy getHandler(Class<? extends ICommand> commandType) {
		return commandAsyncHandlerPool.fetchCommandHandler(commandType);
	}
}