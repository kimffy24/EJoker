package com.jiefzz.ejoker.infrastructure.queue.protocols;

import java.io.Serializable;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.infrastructure.common.utilities.Ensure;

public class Message implements Serializable {

	@PersistentIgnore
	private static final long serialVersionUID = 8472364779319333477L;
	
	public String topic;
	public int code;
	public byte[] body;
	public long timestamp;
	public String tag;
	
	public Message() { }
	public Message(String topic, int code, byte[] body, String tag) {
		this(topic, code, body, System.currentTimeMillis(), tag);
	}
	public Message(String topic, int code, byte[] body, long timestamp) {
		this(topic, code, body, timestamp, null);
	}
	public Message(String topic, int code, byte[] body, long timestamp, String tag) {
        Ensure.notNull(topic, "topic");
        Ensure.positive(code, "code");
        Ensure.notNull(body, "body");
		this.topic = topic;
		this.code = code;
		this.body = body;
		this.timestamp = timestamp;
		this.tag = tag;
	}

	public String toString() {
		return String.format("[Topic=%s,Code=%d,Tag=%s,CreatedTime=%d,BodyLength=%d]", topic, code, tag, timestamp, body.length);
	}
}