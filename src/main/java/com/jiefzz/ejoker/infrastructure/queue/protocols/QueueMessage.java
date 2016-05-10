package com.jiefzz.ejoker.infrastructure.queue.protocols;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.infrastructure.UnimplementException;

public class QueueMessage extends Message {

	@PersistentIgnore
	private static final long serialVersionUID = -6803336272145753874L;
	
	public long logPosition; // { get; set; }
	public String messageId; // { get; set; }
	public int queueId; // { get; set; }
	public long queueOffset; // { get; set; }
	public long storedTime; // { get; set; }

	public QueueMessage() { }
	public QueueMessage(String messageId, String topic, int code, byte[] body, int queueId, long queueOffset, long createdTime, long storedTime, String tag) {
		super(topic, code, body, createdTime, tag);
		this.messageId = messageId;
		this.queueId = queueId;
		this.queueOffset = queueOffset;
		this.storedTime = storedTime;
	}
	
	public void readFrom(byte[] recordBuffer)
    {
        // cpoy from EQueue.Protocols.QueueMessage
		throw new UnimplementException(QueueMessage.class.getName()+"readFrom");
    }
    public boolean isValid()
    {
        return (messageId == null || "".equals(messageId));
    }

    @Override
    public String toString()
    {
        return String.format(
        	"[Topic=%s,QueueId=%d,QueueOffset=%d,MessageId=%s,LogPosition=%d,Code=%d,CreatedTime=%d,StoredTime=%d,BodyLength=%d,Tag=%s]",
            topic,
            queueId,
            queueOffset,
            messageId,
            logPosition,
            code,
            createdTime,
            storedTime,
            body.length,
            tag);
    }

}
