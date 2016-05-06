package com.jiefzz.ejoker.infrastructure.queue.clients.producers;

import com.jiefzz.ejoker.infrastructure.queue.protocols.MessageStoreResult;

public class SendResult {

    private SendStatus sendStatus;
    private MessageStoreResult messageStoreResult;
    private String errorMessage;
    
    public SendResult(SendStatus sendStatus, MessageStoreResult messageStoreResult, String errorMessage)
    {
        this.sendStatus = sendStatus;
        this.messageStoreResult = messageStoreResult;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString()
    {
        return String.format("[SendStatus:%d,MessageStoreResult:%s,ErrorMessage:%s]",
        		sendStatus.ordinal(),
        		messageStoreResult,
        		errorMessage);
    }
}
