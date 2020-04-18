package pro.jk.ejoker.commanding;

public interface ICommandProcessor {

	/**
	 * Process the given command.
	 * @param processingCommand
	 */
	public void process(ProcessingCommand processingCommand);
	
}
