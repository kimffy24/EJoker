package com.jiefzz.ejoker.queue.command;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.queue.IReplyHandler;
import com.jiefzz.ejoker.queue.SendReplyService;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.rpc.simpleRPC.RPCFramework;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.FutureTaskCompletionSource;

@EService
public class CommandResultProcessor implements IReplyHandler, IWorkerService {

	private final static Logger logger = LoggerFactory.getLogger(CommandResultProcessor.class);

	private final Map<String, CommandTaskCompletionSource> commandTaskMap = new ConcurrentHashMap<String, CommandTaskCompletionSource>();

	private AtomicBoolean start = new AtomicBoolean(false);
	
	@Override
	public CommandResultProcessor start() {
		if(!start.compareAndSet(false, true)) {
			logger.warn("{} has started!", this.getClass().getName());
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						RPCFramework rpcFramework = new RPCFramework(CommandResultProcessor.this, SendReplyService.REPLY_PORT);
						rpcFramework.export();
					} catch (Exception e) {
						logger.error("{} faild on start!!!", this.getClass().getName());
						throw new InfrastructureRuntimeException("CommandResultProcessor RPC binding faild!!!", e);
					}
				}
			}).start();
		}
		return this;
	}

	@Override
	public CommandResultProcessor shutdown() {
		logger.error("Actually, we could not shutdown the CommandResultProcessor!!!");
		return this;
	}
	
	public String getBindingAddress(){
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void regiesterProcessingCommand(ICommand command, CommandReturnType commandReturnType,
			FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource) {
		if (null != commandTaskMap.putIfAbsent(command.getId(), new CommandTaskCompletionSource(commandReturnType, taskCompletionSource))) {
			throw new RuntimeException(String.format("Duplicate processing command registion, [type={}, id={}]",
					command.getClass().getName(), command.getId()));
		}
	}

	/**
	 * 直接标记任务失败
	 * @param command
	 */
	public void processFailedSendingCommand(ICommand command) {
		CommandTaskCompletionSource commandTaskCompletionSource;
		if (null != (commandTaskCompletionSource = commandTaskMap.remove(command.getId()))) {
			CommandResult commandResult = new CommandResult(
					CommandStatus.Failed,
					command.getId(),
					command.getAggregateRootId(),
					"Failed to send the command.",
					String.class.getName()
			);

			AsyncTaskResult<CommandResult> asyncTaskResult = new AsyncTaskResult<CommandResult>(AsyncTaskStatus.Success, commandResult);
			commandTaskCompletionSource.taskCompletionSource.task.trySetResult(asyncTaskResult);
		}
	}

	@Override
	public void handlerResult(int type, CommandResult commandResult) {
		
		CommandTaskCompletionSource commandTaskCompletionSource;
		if(null!=(commandTaskCompletionSource = commandTaskMap.getOrDefault(commandResult.getCommandId(), null))) {
			
			if(CommandReturnType.CommandExecuted == commandTaskCompletionSource.getCommandReturnType()) {
				commandTaskMap.remove(commandResult.getCommandId());
				AsyncTaskResult<CommandResult> asyncTaskResult = new AsyncTaskResult<CommandResult>(AsyncTaskStatus.Success, commandResult);
				if(commandTaskCompletionSource.taskCompletionSource.task.trySetResult(asyncTaskResult))
					logger.debug("Command result return, {}", commandResult);
			} else if(CommandReturnType.EventHandled == commandTaskCompletionSource.getCommandReturnType()) {
				if(CommandStatus.Failed == commandResult.getStatus() || CommandStatus.NothingChanged == commandResult.getStatus()) {
					commandTaskMap.remove(commandResult.getCommandId());
					AsyncTaskResult<CommandResult> asyncTaskResult = new AsyncTaskResult<CommandResult>(AsyncTaskStatus.Success, commandResult);
					if(commandTaskCompletionSource.taskCompletionSource.task.trySetResult(asyncTaskResult))
						logger.debug("Command result return, {}", commandResult);
				}
			}
		}
		
	}

	class CommandTaskCompletionSource {

		private final CommandReturnType commandReturnType;
		private final FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource;

		public CommandTaskCompletionSource(CommandReturnType commandReturnType,
				FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource) {
			this.commandReturnType = commandReturnType;
			this.taskCompletionSource = taskCompletionSource;
		}

		public CommandReturnType getCommandReturnType() {
			return commandReturnType;
		}

		public FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> getTaskCompletionSource() {
			return taskCompletionSource;
		}
	}


}
