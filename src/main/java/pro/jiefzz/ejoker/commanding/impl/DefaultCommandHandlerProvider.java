package pro.jiefzz.ejoker.commanding.impl;

import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProvider;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

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