package com.jiefzz.ejoker.commanding;

/**
 * Java could not multi-implement ICommandHandler.<br>
 * 使用直接重载。<br>
 * 同时在此处分析Command对应的CommandHandler<br>
 * @author jiefzz
 *
 */
public abstract class AbstractCommandHandler implements ICommandHandler<ICommand> {
	
	@Override
	public void handle(ICommandContext context, ICommand command) {
		throw new CommandRuntimeException("Do you forget to implement the handler function to handle command which type is " +command.getClass().getName());
	}
}
