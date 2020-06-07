package pro.jk.ejoker.queue;

import java.util.concurrent.Future;

import pro.jk.ejoker.commanding.CommandResult;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONConverter;
import pro.jk.ejoker.common.service.rpc.IRPCService;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.queue.domainEvent.DomainEventHandledMessage;

@EService
public class SendReplyService {

	@Dependence
	private IRPCService rpcService;

	@Dependence
	private IJSONConverter jsonConverter;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	public Future<Void> sendReply(int replyType, CommandResult commandResult, String replyAddress) {
		return systemAsyncHelper.submit(() -> {
			ReplyMessage rm = new ReplyMessage();
			rm.t = replyType;
			rm.c = commandResult;
			sendReplyInternal(replyAddress, rm);
		});
	}
	
	public Future<Void> sendReply(int replyType, DomainEventHandledMessage eomainEventHandledMessage, String replyAddress) {
		return systemAsyncHelper.submit(() -> {
			ReplyMessage rm = new ReplyMessage();
			rm.t = replyType;
			rm.d = eomainEventHandledMessage;
			sendReplyInternal(replyAddress, rm);
		});
	}
	
	private void sendReplyInternal(String replyAddress, ReplyMessage rm) {
		String host;
		int port;
		{
			String[] split = replyAddress.split(":");
			host = split[0];
			if(split.length == 2)
				port = Integer.valueOf(split[1]);
			else
				// 不应该返回配置中的值，应该返回不受配置影响的固定值
				// return EJokerEnvironment.REPLY_PORT;
				port = 25432;
		}
		String convert = jsonConverter.convert(rm);
		rpcService.remoteInvoke(convert, host, port);
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
