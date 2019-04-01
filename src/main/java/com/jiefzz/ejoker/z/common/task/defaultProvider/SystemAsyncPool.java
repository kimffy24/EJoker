package com.jiefzz.ejoker.z.common.task.defaultProvider;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.task.IAsyncEntrance;

public class SystemAsyncPool implements IAsyncEntrance {
	
	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(SystemAsyncPool.class);

	private final ExecutorService defaultThreadPool;
	
	private final int corePollSize;
	
	private final BlockingQueue<Runnable> workQueue;
	
	public SystemAsyncPool(int threadPoolSize) {
		this(threadPoolSize, false);
	}
	
	public SystemAsyncPool(int threadPoolSize, boolean prestartAllThread) {
		corePollSize = threadPoolSize;
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
				threadPoolSize,
				threadPoolSize,
				0l,
				TimeUnit.MILLISECONDS,
				workQueue = new LinkedBlockingQueue<Runnable>(1),
				new ThreadFactory() {
				    private final AtomicLong threadIndex = new AtomicLong(0);
				    private final String threadNamePrefix;
				    private final boolean daemon;

				    @Override
				    public Thread newThread(Runnable r) {
				        Thread thread = new Thread(r, threadNamePrefix + this.threadIndex.incrementAndGet());
				        thread.setDaemon(daemon);
				        return thread;
				    }

				    { // constructor
				        this.threadNamePrefix = "EjokerInnerThread-";
				        this.daemon = false;
				    }
				},
				new ThreadPoolExecutor.CallerRunsPolicy());
		if(prestartAllThread)
			threadPoolExecutor.prestartAllCoreThreads();
		defaultThreadPool = threadPoolExecutor;
		
	}

	@Override
	public <TAsyncTaskResult> Future<TAsyncTaskResult> execute(IFunction<TAsyncTaskResult> asyncTaskThread) {
		if(((ThreadPoolExecutor )defaultThreadPool).getActiveCount() >= corePollSize) {
			// @important 1. 如果当前提交线程本来就是线程池中的线程，则由当前线程直接执行
			// @important 2. 如果当前线程池的任务队列中有等待的任务，则由当前线程直接执行（可以视为线程池满载了，在提交任务前直接执行CallerRunsPolicy策略）
			RipenFuture<TAsyncTaskResult> future = new RipenFuture<>();
			try {
				future.trySetResult(asyncTaskThread.trigger());
			} catch (Exception e) {
				future.trySetException(e);
			}
			return future;
		}
		
		// @important 建立新线程存在线程上限和大量的上下文切换成本，极易发生OutOfMemory。
		// @important 或者使用cachedThreadPool？？
		
		// @important 使用线程池模式的话，正常的情况还好，但是有一个棘手的问题，
		// @important 在整个系统中某一个有超过系统空闲线程（或这个数量级附近时），
		// @important 假设这些存量线程都在开新任务并异步等待结果的话，
		// @important 系统会处于死锁和饿死状态中（对旧线程是死锁，对新任务则是饿死），
		// @important 旧的任务将永远等不到他们的结果，新的任务却没有任何新线程来处理。
		// @important 不治本的解决方法:
		// @important  1. 基于框架的幂等性，我们可以重启程序，重新接受指令。
		// @important  2. 采用线程池占满补偿方案：
		// @important		采用游离线程处理等待中的任务，且不接受超过n毫秒的任务，超时即杀死，并向提交此任务的线程返回异常。
		// @important		* 可能长时间存在游离线程一直被杀死，且提交线程不断重试的情况。
		// @important  3. 不使用异步任务，直接当前线程执行。
		return defaultThreadPool.submit(asyncTaskThread::trigger);
		
		// @important 根治的解决方法使用coroutine方案，目前考虑使用 Quasar 方案
	}

	public void shutdown() {
		defaultThreadPool.shutdown();
	}

}
