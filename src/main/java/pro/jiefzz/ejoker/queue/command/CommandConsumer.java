package pro.jiefzz.ejoker.queue.command;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.commanding.AggregateRootAlreadyExistException;
import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.commanding.CommandReturnType;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandExecuteContext;
import pro.jiefzz.ejoker.commanding.ICommandProcessor;
import pro.jiefzz.ejoker.commanding.ProcessingCommand;
import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IAggregateStorage;
import pro.jiefzz.ejoker.domain.IRepository;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.queue.SendReplyService;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.RipenFuture;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.system.task.context.EJokerTaskAsyncHelper;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.system.exceptions.ArgumentNullException;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureUtil;

@EService
public class CommandConsumer extends AbstractEJokerQueueConsumer {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(CommandConsumer.class);

	@Dependence
	private SendReplyService sendReplyService;

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private ICommandProcessor processor;

	@Dependence
	private IRepository repository;

	@Dependence
	private IAggregateStorage aggregateRootStorage;

	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;

	@Override
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		// Here QueueMessage is a carrier of Command
		// separate it from QueueMessage；
		HashMap<String, String> commandItems = new HashMap<>();
		String messageBody = new String(queueMessage.getBody(), Charset.forName("UTF-8"));
		CommandMessage commandMessage = jsonSerializer.revert(messageBody, CommandMessage.class);
		Class<? extends ICommand> commandType = (Class<? extends ICommand>) typeNameProvider.getType(queueMessage.getTag());
		ICommand command = jsonSerializer.revert(commandMessage.commandData, commandType);
		CommandExecuteContext commandExecuteContext = new CommandExecuteContext(queueMessage, context, commandMessage);
		commandItems.put("CommandReplyAddress", commandMessage.replyAddress);
		processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));
	}

	@Override
	protected long getConsumerLoopInterval() {
		// TODO Auto-generated method stub
		return 2000l;
	}

	/**
	 * commandHandler处理过程中，使用的上下文就是这个上下文。<br>
	 * 他能新增一个聚合根，取出聚合跟，修改聚合并提交发布，都在次上下文中提供调用
	 * 
	 * @author kimffy
	 *
	 */
	class CommandExecuteContext implements ICommandExecuteContext {

		private String result;

		private final Map<String, IAggregateRoot> trackingAggregateRootDict = new HashMap<>();

		private final EJokerQueueMessage message;

		private final IEJokerQueueMessageContext messageContext;

		private final CommandMessage commandMessage;
		
		private IApplicationMessage applicationMessage = null;

		public CommandExecuteContext(EJokerQueueMessage message, IEJokerQueueMessageContext messageContext,
				CommandMessage commandMessage) {
			this.message = message;
			this.commandMessage = commandMessage;
			this.messageContext = messageContext;
		}

		@Override
		public Future<Void> onCommandExecutedAsync(CommandResult commandResult) {
			messageContext.onMessageHandled(message);

			if (null == commandMessage.replyAddress || "".equals(commandMessage.replyAddress))
				return EJokerFutureUtil.completeFuture();

			return sendReplyService.sendReply(CommandReturnType.CommandExecuted.ordinal(), commandResult,
					commandMessage.replyAddress);

		}

		@Override
		public void add(IAggregateRoot aggregateRoot) {
			if (aggregateRoot == null)
				throw new ArgumentNullException("aggregateRoot");
			String uniqueId = aggregateRoot.getUniqueId();
			if (null != trackingAggregateRootDict.putIfAbsent(uniqueId, aggregateRoot))
				throw new AggregateRootAlreadyExistException(uniqueId, aggregateRoot.getClass());
		}

		@Override
		public Future<AsyncTaskResult<Void>> addAsync(IAggregateRoot aggregateRoot) {
			return eJokerAsyncHelper.submit(() -> add(aggregateRoot));
		}

		@Override
		public <T extends IAggregateRoot> Future<T> getAsync(Object id, Class<T> clazz,
				boolean tryFromCache) {

			if (id == null) {
				RipenFuture<T> ripenFuture = new RipenFuture<>();
				ripenFuture.trySetException(new ArgumentNullException("id"));
				return ripenFuture;
			}
			
			return systemAsyncHelper.submit(() -> get(id, clazz, tryFromCache));
		}

		@Override
		public Collection<IAggregateRoot> getTrackedAggregateRoots() {
			return trackingAggregateRootDict.values();
		}

		@Override
		public void clear() {
			trackingAggregateRootDict.clear();
			result = null;
		}

		@Override
		public void setResult(String result) {
			this.result = result;
		}

		@Override
		public String getResult() {
			return result;
		}

		private <T extends IAggregateRoot> T get(Object id, Class<T> clazz, boolean tryFromCache) {

			RipenFuture<T> ripenFuture = new RipenFuture<>();
			// getAsync已做了检查
			// if (id == null) {
			// 	throw new ArgumentNullException("id");
			// }

			String aggregateRootId = id.toString();
			IAggregateRoot aggregateRoot = null;

			// try get aggregate root from the last execute context.
			if (null != (aggregateRoot = trackingAggregateRootDict.get(aggregateRootId)))
				return (T) aggregateRoot;

			if (tryFromCache)
				// TODO @await
				aggregateRoot = await(repository.getAsync((Class<IAggregateRoot>) clazz, id));
			else
				// TODO @await
				aggregateRoot = await(aggregateRootStorage.getAsync((Class<IAggregateRoot>) clazz, aggregateRootId));
			if (aggregateRoot != null) {
				trackingAggregateRootDict.put(aggregateRoot.getUniqueId(), aggregateRoot);
				return (T) aggregateRoot;
			}
			
			return null;
		}

		@Override
		public void setApplicationMessage(IApplicationMessage applicationMessage) {
			this.applicationMessage = applicationMessage;
		}

		@Override
		public IApplicationMessage getApplicationMessage() {
			return applicationMessage;
		}

	}

}
