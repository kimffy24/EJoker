package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.commanding.CommandResult;

public interface IReplyHandler {

	public void handlerResult(int type, CommandResult commandResult);
	
}
