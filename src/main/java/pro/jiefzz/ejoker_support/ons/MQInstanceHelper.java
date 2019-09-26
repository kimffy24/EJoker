package pro.jiefzz.ejoker_support.ons;

import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.consumer.rebalance.AllocateMessageQueueConsistentHash;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.z.context.dev2.IEJokerSimpleContext;
import pro.jiefzz.ejoker.z.scavenger.Scavenger;

public class MQInstanceHelper {

	public final static DefaultMQConsumer createDefaultMQConsumer(String groupName, String nameServ,
			IEJokerSimpleContext eContext) {

		Scavenger scavenger = eContext.get(Scavenger.class);

		DefaultMQConsumer defaultMQConsumer = new DefaultMQConsumer(groupName);

		defaultMQConsumer.getRealConsumer().setNamesrvAddr(nameServ);

		if(EJokerEnvironment.FLOW_CONTROL_ON_PROCESSING)
			defaultMQConsumer.useFlowControlSwitch((mq, amount, round) -> amount >= EJokerEnvironment.MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE);

		// re-balance策略配置，如果不需要则整个switch语句块删掉，默认rocketmq使用的就是AllocateMessageQueueAveragely策略
		switch (EJokerEnvironment.REBALANCE_STRATEGY) {
		case 1:
			defaultMQConsumer.getRealConsumer().setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());
			break;
		case 2:
			defaultMQConsumer.getRealConsumer().setAllocateMessageQueueStrategy(new AllocateMessageQueueConsistentHash());
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

		defaultMQProducer.getRealProducer().setNamesrvAddr(nameServ);

		scavenger.addFianllyJob(defaultMQProducer::shutdown);

		return defaultMQProducer;

	}
}
