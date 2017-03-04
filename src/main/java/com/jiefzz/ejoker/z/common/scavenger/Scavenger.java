package com.jiefzz.ejoker.z.common.scavenger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scavenger {
	
	private final static Logger logger = LoggerFactory.getLogger(Scavenger.class);

	private static List<Runnable> waitingCleanJobs = new ArrayList<Runnable>();
	private static AtomicInteger amountOfJob = new AtomicInteger(0);

	public static void addFianllyJob(Runnable cleanJob) {
		waitingCleanJobs.add(cleanJob);
		amountOfJob.incrementAndGet();
	}

	public static void cleanUp(){
		for(Runnable cleanJob : waitingCleanJobs)
			try {
				cleanJob.run();
				amountOfJob.decrementAndGet();
			} catch (Exception e) {
				e.printStackTrace();
			}
		logger.info("In the end of Invoking Scavenger.cleanUp(), there {} jobs faild.", amountOfJob.get());
		System.gc();
	}
}
