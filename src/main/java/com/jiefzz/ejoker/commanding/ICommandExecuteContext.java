package com.jiefzz.ejoker.commanding;

public interface ICommandExecuteContext extends ICommandContext, ITrackingContext{

	public void onCommandExecuted(CommandResult commandResult);
	
}
