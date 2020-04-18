package pro.jk.ejoker.commanding;

public interface ICommandHandlerProvider {
	
	public ICommandHandlerProxy getHandler(Class<? extends ICommand> commandType);
	
}
