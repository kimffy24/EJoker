package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.queue.ITopicProvider;

public interface ICommandTopicProvider extends ITopicProvider<ICommand> {
	// empty interface 
	// use for the context to injection
}
