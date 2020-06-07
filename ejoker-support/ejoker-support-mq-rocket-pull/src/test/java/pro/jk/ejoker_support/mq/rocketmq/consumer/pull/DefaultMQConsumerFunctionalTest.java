package pro.jk.ejoker_support.mq.rocketmq.consumer.pull;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.wrapper.DiscardWrapper;
import pro.jk.ejoker_support.mq.rocketmq.consumer.pull.DefaultMQConsumer;

public class DefaultMQConsumerFunctionalTest {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultMQConsumerFunctionalTest.class);
	
	private DefaultMQConsumer consumer = null;
	
	private String namesrvAddr = "172.16.1.11:9876";
	
	private String clientIp = "172.16.1.11";
	
	@AfterEach
	public void after() {
		if(null != consumer) {
			consumer.shutdown();
		}
		consumer = null;
		System.gc();
		System.gc();
	}
	
	String[] TAGS = {"TagA", "TagB", "TagC", "TagD", "TagE", "TagF"};
	
	private void send(final int loop) throws Exception {

		DefaultMQProducer producer = new DefaultMQProducer("no-group-p");

		producer.setNamesrvAddr(namesrvAddr);
		producer.setClientIP(clientIp);
        
		producer.start();
		try {
			for (int i = 0; i < loop; i++) {
				Message msg = new Message("TOPIC_TEST_B", // topic
						TAGS[i%6], // tag
						StringUtilx.fmt("Hello RocketMQ, QuickStart [{}], at [{}]", i, new Date()).getBytes()// body
				);
				SendResult sendResult = producer.send(msg);
				logger.info("send result: {}", sendResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		producer.shutdown();
		
	}

	@Test
	public void notest() {}

//	@Test
	public void testA() throws Exception {

		send(256);
		
	}

	@Test
	public void testB() throws Exception {
		
		int loop = 200;
		
		CountDownLatch countDownLatch = new CountDownLatch(loop);
		
		consumer = new DefaultMQConsumer("TestG_BF");

		consumer.setNamesrvAddr(namesrvAddr);
		consumer.setClientIP(clientIp);
		
		consumer.subscribe(
				"TOPIC_TEST_B",
				"TagB||TagF"
				);
		
		consumer.registerMessageHandler((mq, comsumedOffset, code, body, tag, cxt) -> {
			
			logger.info("Mq: {}, Offset: {}, Code: {}, Tag: {}, Body: {}", DefaultMQConsumer.queueUniqueKey(mq), comsumedOffset, code, tag, new String(body));
			
			new Thread(() -> {
				
				int max=60000,min=10000;
				int ran2 = (int) (Math.random()*(max-min)+min);
				
				DiscardWrapper.sleepInterruptable(ran2);
				cxt.onFinished();
			}).start();
			
			countDownLatch.countDown();
			
		});
		
		consumer.enableAutoCommitOffset();
		
		consumer.start();
		
		while(!consumer.isBoostReady()) {
			logger.info("Waiting re_balance.");
			TimeUnit.SECONDS.sleep(1l);
		}
		countDownLatch.await();
		DiscardWrapper.sleepInterruptable(75000l);
	}
	
}
