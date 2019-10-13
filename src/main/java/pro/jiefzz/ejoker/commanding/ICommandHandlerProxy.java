package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface ICommandHandlerProxy extends IObjectProxy {
	
	default public Future<Void> handleAsync(ICommandContext context, ICommand command) {
		throw new RuntimeException("Unimplemented!!!");
	};

}
