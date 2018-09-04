package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.domainEvent.DomainEventHandledMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.rpc.IRPCService;

@EService
public class SendReplyService {

	@Dependence
	IRPCService rpcService;

	@Dependence
	IJSONConverter jsonConverter;
	
	public void sendReply(int replyType, CommandResult commandResult, String replyAddress) {
		ReplyMessage rm = new ReplyMessage();
		rm.t = replyType;
		rm.c = commandResult;
		sendReplyInternal(replyAddress, rm);
	}
	
	public void sendReply(int replyType, DomainEventHandledMessage eomainEventHandledMessage, String replyAddress) {
		ReplyMessage rm = new ReplyMessage();
		rm.t = replyType;
		rm.d = eomainEventHandledMessage;
		sendReplyInternal(replyAddress, rm);
	}
	
	private void sendReplyInternal(String replyAddress, ReplyMessage rm) {
		String convert = jsonConverter.convert(rm);
		rpcService.remoteInvoke(convert, replyAddress, EJokerEnvironment.REPLY_PORT);
	}

	/**
	 * 用于处理节点之间传输处理的消息体格式，使用简短名称节约空间
	 * @author kimffy
	 *
	 */
	public static class ReplyMessage {
		public int t = 0;
		public CommandResult c = null;
		public DomainEventHandledMessage d = null;
	}
}
