package com.jiefzz.ejoker.queue.skeleton.prototype;

import java.io.Serializable;

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
        Ensure.positive(code, "code");
        Ensure.notNull(body, "body");
		this.topic = topic;
		this.code = code;
		this.body = body;
		this.createdTime = createdTime;
		this.tag = tag;
	}

	public String toString() {
		return String.format("[ Topic=%s, Code=%d, Tag=%s, CreatedTime=%d, BodyLength=%d ]", topic, code, tag, createdTime, body.length);
	}
}
