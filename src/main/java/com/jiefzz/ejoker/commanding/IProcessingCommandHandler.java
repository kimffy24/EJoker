package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface IProcessingCommandHandler {

	public SystemFutureWrapper<Void> handle(ProcessingCommand processingCommand);
	
}
