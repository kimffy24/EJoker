package com.jiefzz.ejoker.commanding;

public interface ICommandHandler {

	public abstract <TCommand extends ICommand> void handle(ICommandContext context, TCommand command);
	
}
