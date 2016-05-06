package com.jiefzz.ejoker.infrastructure.queue.protocols;

public class MessageStoreResult {

	public String messageId;
	public int code;
	public String topic;
	public String tag;
	public int queueId;
	public long queueOffset;

	public MessageStoreResult(String messageId, int code, String topic, int queueId, long queueOffset) {
		this(messageId, code, topic, queueId, queueOffset, null);
	}

	public MessageStoreResult(String messageId, int code, String topic, int queueId, long queueOffset, String tag)
	{
		this.messageId = messageId;
		this.code = code;
		this.topic = topic;
		this.tag = tag;
		this.queueId = queueId;
		this.queueOffset = queueOffset;
	}

	@Override
	public String toString()
	{
		return String.format("[MessageId:%s, Code:%d, Topic:%s, QueueId:%d, QueueOffset:%d, Tag:%s]",
				messageId,
				code,
				topic,
				queueId,
				queueOffset,
				tag);
	}

}
