package pro.jk.ejoker_support.rocketmq;

import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;

import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IConsumerWrokerAware;
import pro.jk.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jk.ejoker_support.rocketmq.consumer.pull.DefaultMQConsumer;
import pro.jk.ejoker_support.rocketmq.consumer.pull.DefaultMQConsumer.RocketMQRawMessageHandler;

/**
 * 为分离出与ejoker-core无关的pullConsumer，所以拆解了原来的DefaultMQConsumer
 * ，但是ejoker-core仍然要使用消费者，所以把原来的绑定关系放到这里。
 * @author kimffy
 *
 */
public class DefaultMQConsumerEJokerBindder {
	
	public final static IConsumerWrokerAware getEJokerBundlePullConsumer() {
		return getEJokerBundlePullConsumerDirectly(new DefaultMQConsumer());
	}

	public final static IConsumerWrokerAware getEJokerBundlePullConsumer(RPCHook rpcHook) {
		return getEJokerBundlePullConsumerDirectly(new DefaultMQConsumer(rpcHook));
	}

	public final static IConsumerWrokerAware getEJokerBundlePullConsumer(String consumerGroup) {
		return getEJokerBundlePullConsumerDirectly(new DefaultMQConsumer(consumerGroup));
	}

	public final static IConsumerWrokerAware getEJokerBundlePullConsumer(String consumerGroup, RPCHook rpcHook) {
		return getEJokerBundlePullConsumerDirectly(new DefaultMQConsumer(consumerGroup, rpcHook));
	}

	public final static IConsumerWrokerAware getEJokerBundlePullConsumerDirectly(DefaultMQConsumer pullConsumer) {
		
		return new IConsumerWrokerAware() {
			
			@Override
			public void start() throws Exception {
				pullConsumer.start();
			}

			@Override
			public void shutdown() throws Exception {
				pullConsumer.shutdown();
			}

			@Override
			public void subscribe(String topic, String filter) {
				pullConsumer.subscribe(topic, filter);
			}

			@Override
			public void registerEJokerCallback(IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> vf) {
				pullConsumer.registerEJokerCallback(new RocketMQRawMessageHandler() {
					@Override
					public void handle(MessageQueue mq, long comsumedOffset, int code, byte[] body, String tag,
							IVoidFunction onFinished) {
						vf.trigger(
								new EJokerQueueMessage(mq.getTopic(),  code, body, tag),
								eMsg -> onFinished.trigger()
								);
					}
				});
			}

			@Override
			public void loopInterval() {
				pullConsumer.loopInterval();
			}

			@Override
			public boolean isBoostReady() {
				return pullConsumer.isBoostReady();
			}
			
		};
		
	}
	
}
