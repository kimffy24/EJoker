package com.jiefzz.ejoker.queue.completation;

import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

@EService
public class DefaultMQConsumer extends org.apache.rocketmq.client.consumer.DefaultMQPushConsumer {

	public void registerEJokerCallback(IVoidFunction1<EJokerQueueMessage> vf) {
		registerMessageListener(new MessageListenerConcurrently() {
			@Override
			public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

				for(MessageExt rmqMsg:msgs) {
					EJokerQueueMessage queueMessage = new EJokerQueueMessage(
							rmqMsg.getTopic(),
							rmqMsg.getFlag(),
							rmqMsg.getBody(),
							rmqMsg.getTags());
					vf.trigger(queueMessage);
				}
				
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		});
	}
	
}
