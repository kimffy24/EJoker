package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface ICommandHandlerProxy extends IObjectProxy {
	
	public void handle(ICommandContext context, ICommand command) throws Exception;
	
}
