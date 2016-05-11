package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.IProcessingCommandScheduler;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultProcessingCommandSchedulerImpl implements IProcessingCommandScheduler {

	@Override
	public void scheduleMailbox(ProcessingCommandMailbox mailbox) {
		if (mailbox.enterHandlingMessage()){
			// TODO: do not control thread anymore??
			new Thread(mailbox).start();
		}
	}

}
