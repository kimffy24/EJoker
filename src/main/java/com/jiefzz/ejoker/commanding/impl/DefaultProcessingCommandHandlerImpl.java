package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultProcessingCommandHandlerImpl implements IProcessingCommandHandler {

	@Override
	public void handleAsync(ProcessingCommand processingCommand) {
		throw new UnimplementException(DefaultProcessingCommandHandlerImpl.class.getName()+".handleAsync(ProcessingCommand)");
	}

}
