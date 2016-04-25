package com.jiefzz.ejoker.commanding;

public interface ICommandService {

	public void sendAsync(ICommand command);
	public void send(ICommand command);
	
	public void execute(ICommand command, int timeoutMillis);
	//public void execute(ICommand command, CommandReturnType commandReturnType, int timeoutMillis);
	
	public void executeAsync(ICommand command);
	//public void executeAsync(ICommand command, CommandReturnType commandReturnType);
}
