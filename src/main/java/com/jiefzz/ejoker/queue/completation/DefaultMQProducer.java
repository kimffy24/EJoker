package com.jiefzz.ejoker.queue.completation;

import org.apache.rocketmq.remoting.RPCHook;

public class DefaultMQProducer extends org.apache.rocketmq.client.producer.DefaultMQProducer {

	public DefaultMQProducer() {
		super();
	}

	public DefaultMQProducer(RPCHook rpcHook) {
		super(rpcHook);
	}

	public DefaultMQProducer(String producerGroup, RPCHook rpcHook) {
		super(producerGroup, rpcHook);
	}

	public DefaultMQProducer(String producerGroup) {
		super(producerGroup);
	}

}
