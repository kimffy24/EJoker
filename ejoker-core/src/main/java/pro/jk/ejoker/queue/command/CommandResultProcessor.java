package pro.jk.ejoker.queue.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.EJokerEnvironment;
import pro.jk.ejoker.commanding.CommandResult;
import pro.jk.ejoker.commanding.CommandReturnType;
import pro.jk.ejoker.commanding.CommandStatus;
import pro.jk.ejoker.commanding.ICommand;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONConverter;
import pro.jk.ejoker.common.service.IWorkerService;
import pro.jk.ejoker.common.service.rpc.IClientNodeIPAddressProvider;
import pro.jk.ejoker.common.service.rpc.IRPCService;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jk.ejoker.queue.SendReplyService.ReplyMessage;
import pro.jk.ejoker.queue.domainEvent.DomainEventHandledMessage;

@EService
public class CommandResultProcessor implements IWorkerService {

	private final static Logger logger = LoggerFactory.getLogger(CommandResultProcessor.class);

	@Dependence
	private IRPCService rpcService;

	@Dependence
	private IClientNodeIPAddressProvider clientNodeIPAddressProvider;

	@Dependence
	private IJSONConverter jsonConverter;
	
	private final Map<String, CommandTaskCompletionSource> commandTaskMap = new ConcurrentHashMap<>();

	private AtomicBoolean start = new AtomicBoolean(false);
	
	@Override
	public CommandResultProcessor start() {
		if (!start.compareAndSet(false, true)) {
			logger.warn("{} has started!", this.getClass().getName());
		} else {
			rpcService.export((parameter) -> {
				logger.debug("{} receive msg. [msg: \"{}\"]", parameter);
				ReplyMessage revert = jsonConverter.revert(parameter, ReplyMessage.class);
				if(null!=revert.c)
					CommandResultProcessor.this.handlerResult(revert.t, revert.c);
				else if(null!=revert.d)
					CommandResultProcessor.this.handlerResult(revert.t, revert.d);
			}, EJokerEnvironment.REPLY_PORT, true);
		}
		return this;
	}

	@Override
	public CommandResultProcessor shutdown() {
		if (!start.compareAndSet(true, false)) {
			logger.warn("{} has been shutdown!", this.getClass().getName());
		} else {
			rpcService.removeExport(EJokerEnvironment.REPLY_PORT);
		}
		return this;
	}

	@Override
	public boolean isAllReady() {
		return true;
	}

	public String getBindingAddress() {
		if(25432 - EJokerEnvironment.REPLY_PORT == 0)
			return clientNodeIPAddressProvider.getClientNodeIPAddress();
		return clientNodeIPAddressProvider.getClientNodeIPAddress() + ":" + EJokerEnvironment.REPLY_PORT;
	}

	public void regiesterProcessingCommand(ICommand command, CommandReturnType commandReturnType,
			RipenFuture<CommandResult> taskCompletionSource) {
		CommandTaskCompletionSource commandTaskCompletionSource = new CommandTaskCompletionSource(commandReturnType, taskCompletionSource);
		if (null != commandTaskMap.putIfAbsent(command.getId(), commandTaskCompletionSource)) {
			throw new RuntimeException(StringUtilx.fmt("Duplicate processing command registion!!! [type: {}, id: {}]",
					command.getClass().getName(), command.getId()));
		}
	}

	/**
	 * 直接标记任务失败
	 * 
	 * @param command
	 */
	public void processFailedSendingCommand(ICommand command) {
		CommandTaskCompletionSource commandTaskCompletionSource;
		if (null != (commandTaskCompletionSource = commandTaskMap.remove(command.getId()))) {
			CommandResult commandResult = new CommandResult(CommandStatus.Failed, command.getId(),
					command.getAggregateRootId(), "Failed to send the command.", String.class.getName());

			commandTaskCompletionSource.taskCompletionSource.trySetResult(commandResult);
		}
	}

	private void handlerResult(int type, CommandResult commandResult) {
		CommandTaskCompletionSource commandTaskCompletionSource;
		if (null != (commandTaskCompletionSource = commandTaskMap.getOrDefault(commandResult.getCommandId(), null))) {

			if (CommandReturnType.CommandExecuted.equals(commandTaskCompletionSource.getCommandReturnType())) {
				commandTaskMap.remove(commandResult.getCommandId());
				if (commandTaskCompletionSource.taskCompletionSource.trySetResult(commandResult))
					logger.debug("Command result return. [commandResult: {}]", commandResult);
			} else if (CommandReturnType.EventHandled.equals(commandTaskCompletionSource.getCommandReturnType())) {
				if (CommandStatus.Failed.equals(commandResult.getStatus())
						|| CommandStatus.NothingChanged.equals(commandResult.getStatus())) {
					commandTaskMap.remove(commandResult.getCommandId());
					if (commandTaskCompletionSource.taskCompletionSource.trySetResult(commandResult))
						logger.debug("Command result return. [commandResult: {}]", commandResult);
				}
			}
		}

	}

	private void handlerResult(int type, DomainEventHandledMessage message) {
		String commandId = message.getCommandId();
		CommandTaskCompletionSource commandTaskCompletionSource;
		if (null != (commandTaskCompletionSource = commandTaskMap.getOrDefault(commandId, null))) {
			commandTaskMap.remove(commandId);
			CommandResult commandResult = new CommandResult(CommandStatus.Success, commandId,
					message.getAggregateRootId(), message.getCommandResult(),
					message.getCommandResult() != null ? message.getCommandResult().getClass().getName() : null);
			if (commandTaskCompletionSource.taskCompletionSource
					.trySetResult(commandResult))
				logger.debug("Command result return. [commandResult: {}]", commandResult.toString());
		}
	}

	class CommandTaskCompletionSource {

		private final CommandReturnType commandReturnType;
		
		private final RipenFuture<CommandResult> taskCompletionSource;

		public CommandTaskCompletionSource(CommandReturnType commandReturnType,
				RipenFuture<CommandResult> taskCompletionSource) {
			this.commandReturnType = commandReturnType;
			this.taskCompletionSource = taskCompletionSource;
		}

		public CommandReturnType getCommandReturnType() {
			return commandReturnType;
		}

		public RipenFuture<CommandResult> getTaskCompletionSource() {
			return taskCompletionSource;
		}
	}

}
