package pro.jiefzz.ejoker.commanding;

public interface ICommandHandler<TCommand extends ICommand> {

	public abstract void handle(ICommandContext context, TCommand command);
	
}
