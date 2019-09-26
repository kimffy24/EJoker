package pro.jiefzz.ejoker.commanding.impl;

import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandAsyncHandlerProvider;
import pro.jiefzz.ejoker.commanding.ICommandAsyncHandlerProxy;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandAsyncHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

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