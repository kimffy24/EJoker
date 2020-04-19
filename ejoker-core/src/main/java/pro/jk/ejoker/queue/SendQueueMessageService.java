package pro.jk.ejoker.queue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.EJokerEnvironment;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EInitialize;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.Scavenger;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.task.io.IOExceptionOnRuntime;
import pro.jk.ejoker.common.system.wrapper.MixedThreadPoolExecutor;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IProducerWrokerAware;

@EService
public class SendQueueMessageService {

	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private Scavenger scavenger;
	
	public Future<Void> sendMessageAsync(
			IProducerWrokerAware producer,
			String messageType,
			String messageClass,
			EJokerQueueMessage message,
			String routingKey,
			String messageId,
			Map<String, String> messageExtensionItems)
	{
		
		SendServiceContext cxt = new SendServiceContext(messageType, messageClass, message, routingKey, messageId, messageExtensionItems);

		if (EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SEND) {

			return threadPoolExecutor.submit(() -> {
				try {
					producer.send(message, routingKey, cxt);
					return null;
				} catch (Exception e) {
					throw new IOExceptionOnRuntime(e instanceof IOException ? (IOException )e : new IOException(e));
				}
			});

		} else {

			// use eJoker inner executor service
			return systemAsyncHelper.submit(() -> producer.send(message, routingKey, cxt));
			
		}

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

	public final static class SendServiceContext implements IProducerWrokerAware.ContextAware {

		private final String messageType;
		
		private final String messageClass;
		
		private final EJokerQueueMessage message;
		
		private final String routingKey;
		
		private final String messageId;
		
		private final Map<String, String> messageExtensionItems;

		public SendServiceContext(String messageType, String messageClass, EJokerQueueMessage message,
				String routingKey, String messageId, Map<String, String> messageExtensionItems) {
			super();
			this.messageType = messageType;
			this.messageClass = messageClass;
			this.message = message;
			this.routingKey = routingKey;
			this.messageId = messageId;
			this.messageExtensionItems = messageExtensionItems;
		}
		
		@Override
		public void triggerSuccess() {

			logger.debug(
					"EJoker message send suceess. [topType: {}, message: {}, sendResult: {}, routingKey: {}, messageType: {}, messageId: {}, messageExtensionItems: {}]",
					messageType,
					message,
					"ok",
					routingKey,
					messageClass,
					messageId,
					(null == messageExtensionItems ? "null" : messageExtensionItems.toString())
				);
			
		}

		@Override
		public void triggerFaild(String reason) {
			
			logger.error(
					"EJoker message send failed! [topType: {}, message: {}, sendResult: {}, routingKey: {}, messageType: {}, messageId: {}, messageExtensionItems: {}]",
					messageType,
					message,
					reason,
					routingKey,
					messageClass,
					messageId,
					(null == messageExtensionItems ? "null" : messageExtensionItems.toString())
				);
			
		}

		@Override
		public void triggerException(Exception e) {
			
			logger.error(
					"EJoker message send failed!!! [topType: {}, message: {}, routingKey: {}, messageType: {}, messageId: {}, messageExtensionItems: {}]",
					messageType,
					message,
					routingKey,
					messageClass,
					messageId,
					(null == messageExtensionItems ? "null" : messageExtensionItems.toString()),
					e);
			
		}
		
	}

}
