package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

public interface ICommandHandler<TCommand extends ICommand> {

	public abstract Future<Void> handleAsync(ICommandContext context, TCommand command);
	
}
