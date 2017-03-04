package com.jiefzz.ejoker.queue.skeleton.clients.producer;

//import com.jiefzz.ejoker.z.queue.protocols.MessageStoreResult;

public class SendResult {

    public SendStatus sendStatus;
//    public MessageStoreResult messageStoreResult;
    public String errorMessage;
    
    public SendResult(SendStatus sendStatus, /*MessageStoreResult messageStoreResult, */String errorMessage)
    {
        this.sendStatus = sendStatus;
//        this.messageStoreResult = messageStoreResult;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString()
    {
        return String.format("[SendStatus:%d, MessageStoreResult:%s, ErrorMessage:%s]",
        		sendStatus.ordinal(),
//        		messageStoreResult,
        		errorMessage);
    }
}
