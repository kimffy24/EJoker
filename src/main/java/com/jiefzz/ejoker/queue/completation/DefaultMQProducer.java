package com.jiefzz.ejoker.queue.completation;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;

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
	
	public Future<SendResult> sendAsync(
			Message msg) {
		return threadPoolExecutor.submit(() -> this.defaultMQProducerImpl.send(msg));
	}
	
	private ThreadPoolExecutor threadPoolExecutor;
	
	private void init() {
		threadPoolExecutor = new ThreadPoolExecutor(128, 128, 500l, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
	}

}
