package com.jiefzz.ejoker_support.rocketmq;

import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueConsistentHash;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.dev2.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;

public class MQInstanceHelper {

	public final static DefaultMQConsumer createDefaultMQConsumer(String groupName, String nameServ,
			IEJokerSimpleContext eContext) {

		Scavenger scavenger = eContext.get(Scavenger.class);

		DefaultMQConsumer defaultMQConsumer = new DefaultMQConsumer(groupName);

		defaultMQConsumer.setNamesrvAddr(nameServ);

		if(EJokerEnvironment.FLOW_CONTROL_ON_PROCESSING)
			defaultMQConsumer.useFlowControlSwitch((mq, amount, round) -> amount >= EJokerEnvironment.MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE);

		// re-balance策略配置，如果不需要则整个switch语句块删掉，默认rocketmq使用的就是AllocateMessageQueueAveragely策略
		switch (EJokerEnvironment.REBALANCE_STRATEGY) {
		case 1:
			defaultMQConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());
			break;
		case 2:
			defaultMQConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueConsistentHash());
			break;
		default:
			throw new RuntimeException("Invalid value of REBALANCE_STRATEGY!!!");
		}

		scavenger.addFianllyJob(defaultMQConsumer::shutdown);

		return defaultMQConsumer;

	}

	public final static DefaultMQProducer createDefaultMQProducer(String groupName, String nameServ,
			IEJokerSimpleContext eContext) {
		Scavenger scavenger = eContext.get(Scavenger.class);

		DefaultMQProducer defaultMQProducer = new DefaultMQProducer(groupName);

		defaultMQProducer.setNamesrvAddr(nameServ);

		scavenger.addFianllyJob(defaultMQProducer::shutdown);

		return defaultMQProducer;

	}
}
