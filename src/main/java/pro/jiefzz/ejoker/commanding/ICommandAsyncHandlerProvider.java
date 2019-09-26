package pro.jiefzz.ejoker.commanding;

public interface ICommandAsyncHandlerProvider {
	
	public ICommandAsyncHandlerProxy getHandler(Class<? extends ICommand> commandType);
	
}
