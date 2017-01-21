package com.jiefzz.ejoker.commanding.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.jiefzz.ejoker.commanding.IProcessingCommandScheduler;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultProcessingCommandSchedulerImpl implements IProcessingCommandScheduler {
	
	private static long MAX_SCHEDULE_LIMIT = 500l;

	private AtomicLong schedulingAmount = new AtomicLong(0l);
	
	@Override
	public void scheduleMailbox(ProcessingCommandMailbox mailbox) {
		if(mailbox.hasRemainingCommand() && schedulingAmount.incrementAndGet() <= MAX_SCHEDULE_LIMIT) {
			// TODO: do not control the thread anymore??
			new Thread(mailbox).start();
		} else
			schedulingAmount.decrementAndGet();
	}

	@Override
	public void completeOneSchedule() {
		schedulingAmount.decrementAndGet();
	}

}
