package pro.jk.ejoker.commanding;

import java.util.concurrent.Future;

public interface ICommandService {

	public Future<Void> sendAsync(final ICommand command);
	
	public default Future<CommandResult> executeAsync(final ICommand command) {
		return executeAsync(command, CommandReturnType.CommandExecuted);
	}
	
	public Future<CommandResult> executeAsync(final ICommand command, final CommandReturnType commandReturnType);
	
}
