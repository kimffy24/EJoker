package com.jiefzz.ejoker.queue.completation;

import java.io.Serializable;

import org.apache.rocketmq.common.message.Message;

import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class EJokerQueueMessage implements Serializable {

	@PersistentIgnore
	private static final long serialVersionUID = 8472364779319333477L;

	public String topic;
	public int code;
	public byte[] body;
	public long createdTime;
	public String tag;

	public EJokerQueueMessage() {
	}

	public EJokerQueueMessage(String topic, int code, byte[] body) {
		this(topic, code, body, System.currentTimeMillis(), null);
	}

	public EJokerQueueMessage(String topic, int code, byte[] body, String tag) {
		this(topic, code, body, System.currentTimeMillis(), tag);
	}

	public EJokerQueueMessage(String topic, int code, byte[] body, long createdTime) {
		this(topic, code, body, createdTime, null);
	}

	public EJokerQueueMessage(String topic, int code, byte[] body, long createdTime, String tag) {
		Ensure.notNull(topic, "topic");
		Ensure.nonnegative(code, "code");
		Ensure.notNull(body, "body");
		this.topic = topic;
		this.code = code;
		this.body = body;
		this.createdTime = createdTime;
		this.tag = tag;
	}
	
	public EJokerQueueMessage(Message message) {
		this(message.getTopic(), message.getFlag(), message.getBody(), message.getTags());
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String toString() {
		return String.format("[ Topic=%s, Code=%d, Tag=%s, CreatedTime=%d, BodyLength=%d ]", topic, code, tag, createdTime,
				body.length);
	}
}
