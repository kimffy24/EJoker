package pro.jk.ejoker_support.mq.rocketmq.consumer;

import org.apache.rocketmq.common.message.MessageQueue;

public interface RocketMQRawMessageHandler {

	public void handle(MessageQueue mq, long comsumedOffset, int code, byte[] body, String tag, RocketMQRawMessageHandlingContext cxt);
	
	public static interface RocketMQRawMessageHandlingContext {
		
		public long getCurrentOffset();
		
		public void onFinished();
		
	}
}
