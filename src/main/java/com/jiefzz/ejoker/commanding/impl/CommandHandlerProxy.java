package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandContext;
import com.jiefzz.ejoker.commanding.ICommandHandler;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;

public class CommandHandlerProxy<TCommand extends ICommand> implements ICommandHandlerProxy {

	private final ICommandHandler<TCommand> commandHandler;
	private final Class<ICommandHandler<TCommand>> commandHandlerType;
	
	public CommandHandlerProxy(ICommandHandler<TCommand> commandHandler, Class<ICommandHandler<TCommand>> commandHandlerType) {
		this.commandHandler = commandHandler;
		this.commandHandlerType = commandHandlerType;
	}

	@Override
	public void hadler(ICommandContext context, ICommand command) {
		getInnerObject().handle(context, (TCommand )command);
	}

	@Override
	public ICommandHandler<TCommand> getInnerObject() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
