package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.queue.domainEvent.DomainEventHandledMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.rpc.IRPCService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class SendReplyService {

	@Dependence
	private IRPCService rpcService;

	@Dependence
	private IJSONConverter jsonConverter;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	public SystemFutureWrapper<Void> sendReply(int replyType, CommandResult commandResult, String replyAddress) {
		return systemAsyncHelper.submit(() -> {
			ReplyMessage rm = new ReplyMessage();
			rm.t = replyType;
			rm.c = commandResult;
			sendReplyInternal(replyAddress, rm);
		});
	}
	
	public SystemFutureWrapper<Void> sendReply(int replyType, DomainEventHandledMessage eomainEventHandledMessage, String replyAddress) {
		return systemAsyncHelper.submit(() -> {
			ReplyMessage rm = new ReplyMessage();
			rm.t = replyType;
			rm.d = eomainEventHandledMessage;
			sendReplyInternal(replyAddress, rm);
		});
	}
	
	private void sendReplyInternal(String replyAddress, ReplyMessage rm) {
		String convert = jsonConverter.convert(rm);
		rpcService.remoteInvoke(convert, getHost(replyAddress), getPort(replyAddress));
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
	
	private static String getHost(String replyAddress) {
		String[] split = replyAddress.split(":");
		return split[0];
	}

	private static int getPort(String replyAddress) {
		String[] split = replyAddress.split(":");
		if(split.length == 2)
			return Integer.valueOf(split[1]);
		
		// 不应该返回配置中的值，应该返回不受配置影响的固定值
		// return EJokerEnvironment.REPLY_PORT;
		return 65056;
	}
	
}
