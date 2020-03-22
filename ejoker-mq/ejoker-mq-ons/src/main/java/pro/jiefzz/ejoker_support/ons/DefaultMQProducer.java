package pro.jiefzz.ejoker_support.ons;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.impl.rocketmq.ProducerImpl;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.exception.MQClientException;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.SendResult;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.SendStatus;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.Message;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageQueue;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.remoting.exception.RemotingException;

import pro.jiefzz.ejoker.common.algorithm.ConsistentHashShard;
import pro.jiefzz.ejoker.common.system.enhance.MapUtilx;
import pro.jiefzz.ejoker.common.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.task.io.IOExceptionOnRuntime;
import pro.jiefzz.ejoker.common.system.wrapper.CountDownLatchWrapper;
import pro.jiefzz.ejoker.common.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IProducerWrokerAware;

/**
 * Use consistent hash algorithm to select a queue, as default.<br>
 * * support multi topic.
 * @author kimffy
 *
 */
public class DefaultMQProducer implements IProducerWrokerAware {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQProducer.class);
	
	private final Producer onsProducer;
	
	private final com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.DefaultMQProducer producer;

	public DefaultMQProducer(String groupName) {
		
		Properties producerProperties = new Properties();
        producerProperties.setProperty(PropertyKeyConst.GROUP_ID, "");
        producerProperties.setProperty(PropertyKeyConst.AccessKey, "");
        producerProperties.setProperty(PropertyKeyConst.SecretKey, "");
        producerProperties.setProperty(PropertyKeyConst.NAMESRV_ADDR, "");
        onsProducer = ONSFactory.createProducer(producerProperties);
        producer = ((ProducerImpl )onsProducer).getDefaultMQProducer();
        
	}
	
	public com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.DefaultMQProducer getRealProducer() {
		return producer;
	}

	@Override
	public void send(
			final EJokerQueueMessage message,
			final String routingKey,
			IVoidFunction successAction,
			IVoidFunction1<String> faildAction,
			IVoidFunction1<Exception> exceptionAction) {
//	public void send(final EJokerQueueMessage message, final String routingKey, final String messageId, final String version) {
		Message rMessage = new Message(message.getTopic(), message.getTag(), routingKey, message.getCode(),
				message.getBody(), true);
		// 使用一致性hash选择队列
		SendResult sendResult;
		try {
			sendResult = producer.send(rMessage, this::selectQueue, null);
		} catch (Exception e) {
			exceptionAction.trigger(e);
			throw new IOExceptionOnRuntime(new IOException(e));
		}
		if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())
				&& !SendStatus.SLAVE_NOT_AVAILABLE.equals(sendResult.getSendStatus())) {
				// rocketmq特有情况 如果没有slave可能会报出这个错，但严格来说又不算错。
			faildAction.trigger(sendResult.toString());
			throw new IOExceptionOnRuntime(new IOException(sendResult.toString()));
		}
		successAction.trigger();
	}
	
	@Override
	public void start() throws MQClientException {
		onsProducer.start();
//		producer.start();
	}

	@Override
	public void shutdown() {
		onsProducer.shutdown();
//		producer.shutdown();
	}
	
	private final AtomicInteger noKeysIndex = new AtomicInteger(0);
	
	private Map<String, PredispatchControl> dispatcherDashboard = new ConcurrentHashMap<>();
	
	private MessageQueue selectQueue(List<MessageQueue> mqs, Message msg, Object arg) {
		
		String keys = msg.getKeys();
		if(null == keys || "".equals(keys)) {
			// 无key则轮着发，雨露均沾
			return mqs.get(noKeysIndex.incrementAndGet()%mqs.size());
		}
		
		String topic = msg.getTopic();
		int mqsHashCode = mqs.hashCode();
		PredispatchControl predispatchControl = MapUtilx.getOrAdd(dispatcherDashboard, topic, PredispatchControl::new);
		
		if(mqsHashCode != predispatchControl.lastMqsHashCode.get()) {
			// 抢占 （nameSrv更新broker和queue的状态信息的时间级别基本是秒级的）
			if(predispatchControl.onPasue4RepreparePredispatch.compareAndSet(false, true)) {
				// 抢占成功
				try {
					// 获取生产者队列
					List<MessageQueue> fetchPublishMessageQueues = producer.fetchPublishMessageQueues(topic);
					// 建立哈希环，并更新mqs的hashCode
					predispatchControl.chShard = new ConsistentHashShard<>(fetchPublishMessageQueues);
					predispatchControl.lastMqsHashCode.set(mqsHashCode);
				} catch (Exception e) {
					logger.error("Create ConsistentHashShard faild for topic!!! [topicName: {}]", topic, e);
					e.printStackTrace();
				} finally {
					// 无论哈希环更新/创建成功与否，都要释放等待线程
					predispatchControl.release();
				}
			} else {
				// 抢占失败
				// 等待释放
				predispatchControl.awaitPredispatch();
			}
		}
		
		if(null == predispatchControl.chShard) {
			// 没能建立哈希环的统一视为IO异常，这里包装成运行时IO异常
			throw new IOExceptionOnRuntime(new IOException("ConsistentHashShard create faild!!!"));
		}
		
		// 1. 如果是writeQueueNums变多了，那除了可能会有Timeout外不会有其他异常了
		// 2. 如果是writeQueueNums变少了，那实际上从writeQueueNums发生变化到NameSrv得到更新之间会有一小段时差，
		// 		这段时间差内刚好有消息发送到被离线的queue上可能会收到失败的结果，这种情况应该由消息的提交者控制重试过程。
		// 3. 如果是异常情况（整个系统的那种），从nameSrv得到broker还在正常工作的信息，但事实上broker已经处于不可用状态了，
		//		这个本该是nameSrv的职责，但是这个的一致性哈希算法会一直路由到这个不可用节点的queue上。TODO 没有实际测试，有这种情况再说吧。
		return predispatchControl.chShard.getShardInfo(msg.getKeys());
		
	}
	
	private static class PredispatchControl {
		
		public final static AtomicReferenceFieldUpdater<PredispatchControl, Object> cdlHandleAccesser =
				AtomicReferenceFieldUpdater.newUpdater(PredispatchControl.class, Object.class, "cdlHandle");
		
		public final AtomicInteger lastMqsHashCode = new AtomicInteger(0);
		
		public final AtomicBoolean onPasue4RepreparePredispatch = new AtomicBoolean(false);
		
		public ConsistentHashShard<MessageQueue> chShard = null;
		
		@SuppressWarnings("unused")
		private volatile Object cdlHandle = CountDownLatchWrapper.newCountDownLatch();
		
		public void release() {
			
			CountDownLatchWrapper.countDown(cdlHandleAccesser.get(this));
			onPasue4RepreparePredispatch.set(false);
			
			// waiting for a moment
			DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, 50l);
		}
		
		public void awaitPredispatch() {
			try {
				CountDownLatchWrapper.await(cdlHandleAccesser.get(this));
			} catch (InterruptedException e) {
				throw new AsyncWrapperException(e);
			}
		}
	}
	
}
