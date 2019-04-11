package com.jiefzz.ejoker.queue;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import com.jiefzz.ejoker.queue.aware.IProducerWrokerAware;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.wrapper.MixedThreadPoolExecutor;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

@EService
public class SendQueueMessageService {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private Scavenger scavenger;

	public SystemFutureWrapper<AsyncTaskResult<Void>> sendMessageAsync(IProducerWrokerAware producer,
			final EJokerQueueMessage message, final String routingKey, final String messageId, final String version) {

		if (EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SEND) {

			// use producer inner executor service to execute aSync task 
			// and wrap the result with type SystemFutureWrapper.
			return new SystemFutureWrapper<>(submitWithInnerExector(() -> {
				try {
					producer.send(message, routingKey, messageId, version);
					return AsyncTaskResult.Success;
				} catch (Exception e) {
					return new AsyncTaskResult<>(AsyncTaskStatus.IOException, e.getMessage(), null);
				}
			}));

		} else {

			// use eJoker inner executor service
			return eJokerAsyncHelper.submit(() -> producer.send(message, routingKey, messageId, version));
			
		}

	}
	
	public <T> Future<T> submitWithInnerExector(IFunction<T> vf) {
		return threadPoolExecutor.submit(vf::trigger);
	}
	
	private ThreadPoolExecutor threadPoolExecutor;
	
	@EInitialize
	private void init() {
		if(EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SEND) {
			threadPoolExecutor = new MixedThreadPoolExecutor(
					EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE,
					EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE,
					0l,
					TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(),
					new SendThreadFactory());
			threadPoolExecutor.prestartAllCoreThreads();
			scavenger.addFianllyJob(threadPoolExecutor::shutdown);
		}
	}

	private final static class SendThreadFactory implements ThreadFactory {

		private final static AtomicInteger poolIndex = new AtomicInteger(0);

		private final AtomicInteger threadIndex = new AtomicInteger(0);

		private final ThreadGroup group;

		private final String namePrefix;

		public SendThreadFactory() {

			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "EJokerSender-" + poolIndex.incrementAndGet() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(null, r, namePrefix + threadIndex.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);

			return t;
		}

	}


}
