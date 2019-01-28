package com.jiefzz.ejoker.commanding;

public interface ICommandAsyncHandler<TCommand extends ICommand> {

	public abstract Object handleAsync(ICommandContext context, TCommand command);
	
	public abstract Object handleAsync(TCommand command);
	
}
