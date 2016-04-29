package com.jiefzz.ejoker.commanding;

public class AbstractCommandHandler implements ICommandHandler {

	@Override
	public <TCommand extends ICommand> void handle(ICommandContext context, TCommand command) {
		throw new CommandRuntimeException("Did you forget declare a method to handler command with type ["+command.getClass().getName()+"]");
	}

}
