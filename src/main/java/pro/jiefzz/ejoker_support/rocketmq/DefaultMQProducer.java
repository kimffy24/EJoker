package pro.jiefzz.ejoker_support.rocketmq;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.aware.IProducerWrokerAware;
import pro.jiefzz.ejoker.z.algorithm.ConsistentHashShard;
import pro.jiefzz.ejoker.z.io.IOExceptionOnRuntime;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.wrapper.CountDownLatchWrapper;
import pro.jiefzz.ejoker.z.system.wrapper.SleepWrapper;

/**
 * Use consistent hash algorithm to select a queue, as default.<br>
 * * support multi topic.
 * @author kimffy
 *
 */
public class DefaultMQProducer extends org.apache.rocketmq.client.producer.DefaultMQProducer implements IProducerWrokerAware {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQProducer.class);
	

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

	@Override
	public void send(final EJokerQueueMessage message, final String routingKey, final String messageId, final String version) {
		Message rMessage = new Message(message.getTopic(), message.getTag(), routingKey, message.getCode(),
				message.getBody(), true);
		// 使用一致性hash选择队列
		SendResult sendResult;
		try {
			sendResult = super.send(rMessage, this::selectQueue, null);
		} catch (Exception e) {
			logger.error(
					"EJoker message async send failed, message: {}, routingKey: {}, messageId: {}, version: {}",
					e.getMessage(), rMessage.getKeys(), messageId, version);
			throw new IOExceptionOnRuntime(new IOException(e));
		}
		if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
			if(SendStatus.SLAVE_NOT_AVAILABLE.equals(sendResult.getSendStatus())) {
				// rocketmq特有情况 如果没有slave可能会报出这个错，但严格来说又不算错。
//				logger.warn("RocketMQ slave is no avaliable, but it dosn't matter. sendResult: {}", sendResult);
				return;
			}
			logger.error(
					"EJoker message async send failed, sendResult: {}, routingKey: {}, messageId: {}, version: {}",
					sendResult.toString(), rMessage.getKeys(), messageId, version);
			throw new IOExceptionOnRuntime(new IOException(sendResult.toString()));
		}
	}
	
	@Override
	public void start() throws MQClientException {
		super.start();
	}

	@Override
	public void shutdown() {
		super.shutdown();
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
		PredispatchControl predispatchControl = MapHelper.getOrAddConcurrent(dispatcherDashboard, topic, PredispatchControl::new);
		
		if(mqsHashCode != predispatchControl.lastMqsHashCode.get()) {
			// 抢占 （nameSrv更新broker和queue的状态信息的时间级别基本是秒级的）
			if(predispatchControl.onPasue4RepreparePredispatch.compareAndSet(false, true)) {
				// 抢占成功
				try {
					// 获取生产者队列
					List<MessageQueue> fetchPublishMessageQueues = this.fetchPublishMessageQueues(topic);
					// 建立哈希环，并更新mqs的hashCode
					predispatchControl.chShard = new ConsistentHashShard<>(fetchPublishMessageQueues);
					predispatchControl.lastMqsHashCode.set(mqsHashCode);
				} catch (Exception e) {
					logger.error(String.format("Create ConsistentHashShard faild for topic[name=%s]!!!", topic), e);
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
			SleepWrapper.sleep(TimeUnit.MILLISECONDS, 50l);
		}
		
		public void awaitPredispatch() {
			CountDownLatchWrapper.await(cdlHandleAccesser.get(this));
		}
	}
	
}
