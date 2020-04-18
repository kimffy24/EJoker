package pro.jk.ejoker.queue.skeleton.aware;

import java.io.Serializable;

import pro.jk.ejoker.common.system.enhance.StringUtilx;

public class EJokerQueueMessage implements Serializable {

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
		if(null == topic || "".equals(topic))
			throw new NullPointerException("topic");
		if(null == body || 0 == body.length)
			throw new NullPointerException("body");
		if(0 > code)
			throw new IllegalArgumentException("code should be non negative.");
		this.topic = topic;
		this.code = code;
		this.body = body;
		this.createdTime = createdTime;
		this.tag = tag;
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
		return StringUtilx.fmt("\\{topic={}, code={}, tag={}, createdTime={}, bodyLength={}\\}",
				topic, code, tag, createdTime, body.length);
	}
}
