package pro.jiefzz.ejoker.queue;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.common.context.annotation.context.EService;
import pro.jiefzz.ejoker.common.service.Scavenger;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.common.system.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.common.system.task.context.EJokerTaskAsyncHelper;
import pro.jiefzz.ejoker.common.system.wrapper.MixedThreadPoolExecutor;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IProducerWrokerAware;

@EService
public class SendQueueMessageService {

	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private Scavenger scavenger;
	
	public Future<AsyncTaskResult<Void>> sendMessageAsync(
			IProducerWrokerAware producer,
			String messageType,
			String messageClass,
			EJokerQueueMessage message,
			String routingKey,
			String messageId,
			Map<String, String> messageExtensionItems)
	{
		IVoidFunction sa = () -> {
			logger.debug(
					"EJoker {} message send suceess, message: {}, sendResult: {}, routingKey: {}, messageType: {}, messageId: {}, messageExtensionItems: {}",
					messageType,
					message,
					"ok",
					routingKey,
					messageClass,
					messageId,
					messageExtensionItems.toString()
				);
		};
		
		IVoidFunction1<String> fa = s -> {
			logger.error(
					"EJoker {} message send failed, message: {}, sendResult: {}, routingKey: {}, messageType: {}, messageId: {}, messageExtensionItems: {}",
					messageType,
					message,
					s,
					routingKey,
					messageClass,
					messageId,
					messageExtensionItems.toString()
				);
		};

		IVoidFunction1<Exception> ea = e -> {
			logger.error(String.format(
					"EJoker %s message send failed, message: %s, routingKey: %s, messageType: %s, messageId: %s, messageExtensionItems: %s",
					messageType,
					message,
					routingKey,
					messageClass,
					messageId,
					messageExtensionItems.toString()),
				e);
		};

		if (EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SEND) {

			return submitWithInnerExector(() -> {
				try {
					producer.send(message, routingKey, sa, fa, ea);
					return AsyncTaskResult.Success;
				} catch (Exception e) {
					return new AsyncTaskResult<>(AsyncTaskStatus.IOException, e.getMessage(), null);
				}
			});

		} else {

			// use eJoker inner executor service
			return eJokerAsyncHelper.submit(() -> producer.send(message, routingKey, sa, fa, ea));
			
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
