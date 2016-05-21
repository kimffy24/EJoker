package com.jiefzz.ejoker.z.common.scavenger;

import java.util.ArrayList;
import java.util.List;

public class Scavenger {

	private static List<Runnable> waitingCleanJobs = new ArrayList<Runnable>();

	public static void addFianllyJob(Runnable cleanJob) {
		waitingCleanJobs.add(cleanJob);
	}

	public static void cleanUp(){
		for(Runnable cleanJob : waitingCleanJobs)
			try {
				cleanJob.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
