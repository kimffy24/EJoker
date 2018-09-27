package com.jiefzz.ejoker.z.common.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

public class ReactWorker {
	
	private final static Logger logger = LoggerFactory.getLogger(ReactWorker.class);
	
	private final IVoidFunction job;
	
	private final IVoidFunction1<Throwable> exceptionHandler;

	private volatile Thread workerThread = null;
	
	private AtomicBoolean onRunning = new AtomicBoolean(false);
	
	private AtomicBoolean onPasue = new AtomicBoolean(true);
	
	public ReactWorker(IVoidFunction vf, IVoidFunction1<Throwable> exceptionHandler) {
		job = () -> doWork(vf);
		this.exceptionHandler = exceptionHandler;
	}
	
	public ReactWorker(IVoidFunction vf) {
		this(vf, e -> logger.error("Uncauhgt exception!!!", e));
	}
	
	public void start() {

		if(onRunning.compareAndSet(false, true)) {
			for( int i = 0; null != workerThread && workerThread.isAlive(); i++) {
				if(i>15) {
					onRunning.compareAndSet(true, false); // roll back the if statement.
					throw new RuntimeException("The prevous thread is still on running!!!");
				}
				logger.debug("Sleep 1 second, on {} time(s). Waiting the prevous thread exit...", i);
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (Exception e) {}
			}
			workerThread = new Thread(() -> job.trigger());
			workerThread.start();
		}
	}
	
	public void stop() {
		// TODO !!!!
		resume();
		onRunning.compareAndSet(true, false);
	}
	
	public void pasue() {
		if(onRunning.get())
			onPasue.compareAndSet(false, true);
	}
	
	public void resume() {
		if(onRunning.get() && onPasue.compareAndSet(true, false)) {
			LockSupport.unpark(workerThread);
		}
	}
	
	public boolean resumeState() {
		return !onPasue.get();
	}
	
	public boolean lifeState() {
		return onRunning.get();
	}
	
	private void doWork(IVoidFunction vf) {
		while(onRunning.get()) {
			logger.debug("onRunning.get() ={}", onRunning.get());
			while(onPasue.get()) {
				logger.debug("onPasue.get() ={}", onRunning.get());
				LockSupport.park(workerThread);
			}
			
			try {
				vf.trigger();
			} catch (Exception e) {
				exceptionHandler.trigger(e);
			}
			
		}
		workerThread = null;
	}
}
