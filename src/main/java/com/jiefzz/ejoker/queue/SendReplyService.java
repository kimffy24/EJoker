package com.jiefzz.ejoker.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.queue.domainEvent.DomainEventHandledMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.rpc.simpleRPC.RPCFramework;

@EService
public class SendReplyService {

	private final static Logger logger = LoggerFactory.getLogger(SendReplyService.class);
	
	public void sendReply(int replyType, CommandResult commandResult, String replyAddress) {
		try {
			IReplyHandler replyHandler = RPCFramework.refer(IReplyHandler.class, replyAddress, REPLY_PORT);
			replyHandler.handlerResult(replyType, commandResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendReply(int replyType, DomainEventHandledMessage eomainEventHandledMessage, String replyAddress) {
		try {
			IReplyHandler replyHandler = RPCFramework.refer(IReplyHandler.class, replyAddress, REPLY_PORT);
			replyHandler.handlerResult(replyType, eomainEventHandledMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public final static int REPLY_PORT;
	static {
		// 请从配置文件注入此变量。
		REPLY_PORT = 65056;
	}
}
