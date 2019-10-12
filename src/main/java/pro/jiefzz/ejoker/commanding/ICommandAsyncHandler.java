package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

public interface ICommandAsyncHandler<TCommand extends ICommand> {

	public abstract Future<Void> handleAsync(ICommandContext context, TCommand command);
	
	public abstract Future<Void> handleAsync(TCommand command);
	
}
