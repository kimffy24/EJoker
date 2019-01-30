package com.jiefzz.ejoker.queue.completation;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;

public class DefaultMQProducer extends org.apache.rocketmq.client.producer.DefaultMQProducer {

	public DefaultMQProducer() {
		super();
		init();
	}

	public DefaultMQProducer(RPCHook rpcHook) {
		super(rpcHook);
		init();
	}

	public DefaultMQProducer(String producerGroup, RPCHook rpcHook) {
		super(producerGroup, rpcHook);
		init();
	}

	public DefaultMQProducer(String producerGroup) {
		super(producerGroup);
		init();
	}
	
	@Deprecated
	public Future<SendResult> sendAsync(Message msg) {
		return threadPoolExecutor.submit(() -> this.defaultMQProducerImpl.send(msg));
	}
	
	public <T> Future<T> submitWithInnerExector(IFunction<T> vf) {
		return threadPoolExecutor.submit(vf::trigger);
	}
	
	private ThreadPoolExecutor threadPoolExecutor;
	
	private void init() {
		threadPoolExecutor = new ThreadPoolExecutor(
				EJokerEnvironment.ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE,
				EJokerEnvironment.ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE,
				0l,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(),
				new SendThreadFactory(),
				new ThreadPoolExecutor.CallerRunsPolicy());
		if(EJokerEnvironment.ASYNC_MESSAGE_SENDER_THREADPOLL_PRESTART_ALL)
			threadPoolExecutor.prestartAllCoreThreads();
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
