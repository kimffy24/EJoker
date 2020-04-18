package pro.jk.ejoker.common.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jk.ejoker.common.system.functional.IVoidFunction;

@EService
public class Scavenger {
	
	private final static Logger logger = LoggerFactory.getLogger(Scavenger.class);

	private List<IVoidFunction> waitingCleanJobs = new ArrayList<>();
	
	private AtomicInteger amountOfJob = new AtomicInteger(0);
	
	private final Thread cleanUpThread;
	
	private final CountDownLatch cdl = new CountDownLatch(1);
	
	public Scavenger() {
		cleanUpThread = new Thread(() -> {
			while(cdl.getCount() != 0l) {
				// 此线程拒绝中断
				try {
					cdl.await();
				} catch (InterruptedException e) { }
			}
			int totalJob = amountOfJob.get();
			for(IVoidFunction cleanJob : waitingCleanJobs) {
				try {
					cleanJob.trigger();
					amountOfJob.decrementAndGet();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.debug("In the end of Invoking Scavenger.cleanUp(). [execute: {}, faild: {}]", totalJob, amountOfJob.get());
			System.gc();
		}, "Scavenger-0");
		cleanUpThread.setDaemon(true);
		cleanUpThread.start();
	}

	public void addFianllyJob(IVoidFunction cleanJob) {
		waitingCleanJobs.add(cleanJob);
		amountOfJob.incrementAndGet();
	}

	@Dependence
	private IEjokerContextDev2 ejokerContext;
	
	@EInitialize
	private void init() {
		ejokerContext.destroyRegister(cdl::countDown);
	}
	
}
