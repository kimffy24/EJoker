package pro.jk.ejoker.queue.command;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.commanding.AggregateRootAlreadyExistException;
import pro.jk.ejoker.commanding.CommandResult;
import pro.jk.ejoker.commanding.CommandReturnType;
import pro.jk.ejoker.commanding.ICommand;
import pro.jk.ejoker.commanding.ICommandExecuteContext;
import pro.jk.ejoker.commanding.ICommandProcessor;
import pro.jk.ejoker.commanding.ProcessingCommand;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONConverter;
import pro.jk.ejoker.common.system.exceptions.ArgumentNullException;
import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IAggregateStorage;
import pro.jk.ejoker.domain.IRepository;
import pro.jk.ejoker.domain.domainException.AggregateRootReferenceChangedException;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;
import pro.jk.ejoker.messaging.IApplicationMessage;
import pro.jk.ejoker.queue.SendReplyService;
import pro.jk.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;

@EService
public class CommandConsumer extends AbstractEJokerQueueConsumer {

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

	@Override
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		// Here QueueMessage is a carrier of Command
		// separate it from QueueMessage；
		String messageBody = new String(queueMessage.getBody(), Charset.forName("UTF-8"));
		logger.debug("Received command queue message. [queueMessage: {}, body: {}]", queueMessage, messageBody);
		
		HashMap<String, String> commandItems = new HashMap<>();
		CommandMessage commandMessage = jsonSerializer.revert(messageBody, CommandMessage.class);
		Class<? extends ICommand> commandType = (Class<? extends ICommand>) typeNameProvider.getType(queueMessage.getTag());
		ICommand command = jsonSerializer.revert(commandMessage.commandData, commandType);
		CommandExecuteContext commandExecuteContext = new CommandExecuteContext(queueMessage, context, commandMessage);
		commandItems.put("CommandReplyAddress", commandMessage.replyAddress);
		processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));
	}

	@Override
	protected long getConsumerLoopInterval() {
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

		private final Map<String, AggreagateTrackingTuple> trackingAggregateRootDict = new HashMap<>();

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
			if (null != trackingAggregateRootDict.putIfAbsent(uniqueId, AggreagateTrackingTuple.of(aggregateRoot)))
				throw new AggregateRootAlreadyExistException(uniqueId, aggregateRoot.getClass());
		}

		@Override
		public Future<Void> addAsync(IAggregateRoot aggregateRoot) {
			return systemAsyncHelper.submit(() -> add(aggregateRoot));
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
			return trackingAggregateRootDict.values().stream().map(u -> u.aggr).collect(Collectors.toList());
		}

		@Override
		public void clear() {
			trackingAggregateRootDict.clear();
			result = null;
			applicationMessage = null;
		}

		@Override
		public void ensureTrackedAggregateNotPolluted() {
			trackingAggregateRootDict.values().forEach(t -> {
				if(t.aggr.getVersion() != t.version) {
					throw new AggregateRootReferenceChangedException(t.aggr);
				}
			});
		}

		@Override
		public void setResult(String result) {
			this.result = result;
		}

		@Override
		public String getResult() {
			return result;
		}

		@Override
		public void setApplicationMessage(IApplicationMessage applicationMessage) {
			this.applicationMessage = applicationMessage;
		}

		@Override
		public IApplicationMessage getApplicationMessage() {
			return applicationMessage;
		}

		private <T extends IAggregateRoot> T get(Object id, Class<T> clazz, boolean tryFromCache) {

			// getAsync已做了检查
			// if (id == null) {
			// 	throw new ArgumentNullException("id");
			// }

			String aggregateRootId = id.toString();

			AggreagateTrackingTuple u;
			// try get aggregate root from the last execute context.
			if (null != (u = trackingAggregateRootDict.get(aggregateRootId)))
				return (T) u.aggr;
			
			IAggregateRoot aggregateRoot = null;

			if (tryFromCache)
				// TODO @await
				aggregateRoot = await(repository.getAsync((Class<IAggregateRoot>) clazz, id));
			else
				// TODO @await
				aggregateRoot = await(aggregateRootStorage.getAsync((Class<IAggregateRoot>) clazz, aggregateRootId));
			if (aggregateRoot != null) {
				trackingAggregateRootDict.put(aggregateRoot.getUniqueId(), AggreagateTrackingTuple.of(aggregateRoot));
				return (T) aggregateRoot;
			}
			
			return null;
		}

	}
	
	public final static class AggreagateTrackingTuple {
		public final IAggregateRoot aggr;
		public final long version;
		public AggreagateTrackingTuple(IAggregateRoot aggr, long version) {
			this.aggr = aggr;
			this.version = version;
		}
		public static AggreagateTrackingTuple of(IAggregateRoot aggr, long version) {
			return new AggreagateTrackingTuple(aggr, version);
		}
		public static AggreagateTrackingTuple of(IAggregateRoot aggr) {
			return of(aggr, aggr.getVersion());
		}
	}

}
