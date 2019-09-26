package pro.jiefzz.ejoker.eventing.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.jiefzz.ejoker.eventing.EventCommittingContext;
import pro.jiefzz.ejoker.infrastructure.AbstractMailBox;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

public class EventMailBox extends AbstractMailBox<EventCommittingContext, Void> {

	private final Map<String, Map<String, Byte>> aggregateDictDict = new ConcurrentHashMap<>();;
	
	public EventMailBox(String routingKey, int batchSize, IVoidFunction1<List<EventCommittingContext>> handleMessageAction,
			SystemAsyncHelper systemAsyncHelper) {
		super(routingKey, batchSize, true, null,
				x -> {
					handleMessageAction.trigger(x);
					return SystemFutureWrapperUtil.completeFuture();
				},
				systemAsyncHelper);
		
	}

	@Override
	public void enqueueMessage(EventCommittingContext message) {
		Map<String, Byte> eventDict = MapHelper.getOrAddConcurrent(aggregateDictDict, message.getEventStream().getAggregateRootId(), ConcurrentHashMap::new);
		// 添加成功，则...
		if(null == eventDict.putIfAbsent(message.getEventStream().getId(), (byte )1))
			super.enqueueMessage(message);
	}

	@Override
	public SystemFutureWrapper<Void> completeMessage(EventCommittingContext message, Void result) {
		await(super.completeMessage(message, result));
		removeEventCommittingContext(message);
		return SystemFutureWrapperUtil.completeFuture();
	}

	@Override
	protected List<EventCommittingContext> filterMessages(List<EventCommittingContext> messages) {
		List<EventCommittingContext> filterCommittingContextList = new ArrayList<>();
		if(null != messages && 0 < messages.size()) {
			for(EventCommittingContext committingContext : messages) {
				if(containsEventCommittingContext(committingContext)) {
					filterCommittingContextList.add(committingContext);
				}
			}
		}
		return filterCommittingContextList;
	}
	
	public boolean containsEventCommittingContext(EventCommittingContext eventCommittingContext) {
		Map<String, Byte> eventDict;
		if(null != (eventDict = aggregateDictDict.getOrDefault(eventCommittingContext.getEventStream().getAggregateRootId(), null))) {
			return eventDict.containsKey(eventCommittingContext.getEventStream().getId());
		}
		return false;
	}
	
	public void removeAggregateAllEventCommittingContexts(String aggregateRootId) {
		aggregateDictDict.remove(aggregateRootId);
	}

	public void removeEventCommittingContext(EventCommittingContext eventCommittingContext) {
		Map<String, Byte> eventDict;
		if(null != (eventDict = aggregateDictDict.getOrDefault(eventCommittingContext.getEventStream().getAggregateRootId(), null))) {
			eventDict.remove(eventCommittingContext.getEventStream().getId());
		}
	}
}
