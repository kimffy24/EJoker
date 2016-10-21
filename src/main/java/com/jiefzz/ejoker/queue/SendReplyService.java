package com.jiefzz.ejoker.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.z.queue.IWokerService;

@EService
public class SendReplyService implements IWokerService {

	final static Logger logger = LoggerFactory.getLogger(SendReplyService.class);			
			
	@Override
	public IQueueWokerService start() {
		return null;
	}

	@Override
	public IQueueWokerService shutdown() {
		return null;
	}

	public void sendReply(short replyType, Object replyData) {
		logger.warn("[{}.sendReply() is unimplemented! pass: [ replyType={}, replyData={} ]", this.getClass().getName(), replyType, replyData);
	}
	
}
