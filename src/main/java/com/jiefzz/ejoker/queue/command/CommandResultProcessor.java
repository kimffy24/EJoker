package com.jiefzz.ejoker.queue.command;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.remoting.IRequestHandler;
import com.jiefzz.ejoker.z.common.remoting.RemotingRequest;
import com.jiefzz.ejoker.z.common.system.extension.FutureTaskCompletionSource;

@EService
public class CommandResultProcessor implements IRequestHandler {

	private final static Logger logger = LoggerFactory.getLogger(CommandResultProcessor.class);
	
	private final static byte[] byteArray = new byte[0];
	
	private final static ConcurrentHashMap<String, CommandTaskCompletionSource> commandTaskDict = new ConcurrentHashMap<String, CommandTaskCompletionSource>();
	
	@Resource
	IJSONConverter jsonConverter;
	
	private boolean start = false;
	
	public CommandResultProcessor() { }
	public CommandResultProcessor(Object x) { }
	
	public CommandResultProcessor start() {
		if(start) return this;
		
		/**/
		
		return this;
	}
	
	public CommandResultProcessor shutdown() {
		return this;
	}
	
	public void regiesterProcessingCommand(ICommand command, CommandReturnType commandReturnType, FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource) {
		if(null!=commandTaskDict.putIfAbsent(command.getId(), new CommandTaskCompletionSource(commandReturnType, taskCompletionSource))) {
			throw new RuntimeException(String.format("Duplicate processing command registion, [type={}, id={}]", command.getClass().getName(), command.getId()));
		}
	}
	
	/**
	 * 未完成
	 * @param command
	 */
	public void processFailedSendingCommand(ICommand command) {
		CommandTaskCompletionSource commandTaskCompletionSource;
		if(null != (commandTaskCompletionSource = commandTaskDict.remove(command.getId()))) {
			CommandResult commandResult = new CommandResult(CommandStatus.Failed, command.getId(), command.getAggregateRootId(), "Failed to send the command.", String.class.getName());
			
			// TODO 没找到方法将commandTaskCompletionSource中的FutureTaskCompletionSource中的task置为失败，并将commandResult变量作为失败结果放入。
			logger.debug("没找到方法将commandTaskCompletionSource中的FutureTaskCompletionSource中的task置为失败，并将commandResult变量作为失败结果放入。");
			
			try {
				commandTaskCompletionSource.getTaskCompletionSource().task.get(50, TimeUnit.MICROSECONDS);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void handlerRequest(Object context, RemotingRequest remotingRequest) {
		throw new UnimplementException(this.getClass().getName()+".handlerRequest(Object context, RemotingRequest remotingRequest)");
	}
	
	class CommandTaskCompletionSource {

		public CommandReturnType commandReturnType;
		public FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource;
		
		public CommandTaskCompletionSource(CommandReturnType commandReturnType, FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource) {
			this.commandReturnType = commandReturnType;
			this.taskCompletionSource = taskCompletionSource;
		}
		
		public CommandReturnType getCommandReturnType() {
			return commandReturnType;
		}
		public void setCommandReturnType(CommandReturnType commandReturnType) {
			this.commandReturnType = commandReturnType;
		}
		public FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> getTaskCompletionSource() {
			return taskCompletionSource;
		}
		public void setTaskCompletionSource(FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource) {
			this.taskCompletionSource = taskCompletionSource;
		}
	}
}
