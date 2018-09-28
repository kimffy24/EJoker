package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventAppendResult;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

@EService
public class InMemoryEventStore implements IEventStore {

	private final static Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);

	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private IEventSerializer eventSerializer;

	public Map<String, Object> mStorage = new ConcurrentHashMap<String, Object>();

	@Override
	public boolean isSupportBatchAppendEvent() {
		// TODO 暂时不支持批量持久化
		return false;
	}

	@Override
	public void setSupportBatchAppendEvent(boolean supportBatchAppendEvent) {
	}

	@Override
	public void batchAppendAsync(LinkedHashSet<DomainEventStream> eventStreams) {
		Iterator<DomainEventStream> iterator = eventStreams.iterator();
		while (iterator.hasNext())
			appendAsync(iterator.next());
	}

	private AtomicLong atLong = new AtomicLong(0);

	@Override
	public Future<AsyncTaskResultBase> appendAsync(DomainEventStream eventStream) {
		logger.debug("模拟io! 执行次数: {}, EventStream: {}.", atLong.incrementAndGet(), eventStream.toString());
		EventAppendResult eventAppendResult = appendsync(eventStream);
		RipenFuture<AsyncTaskResultBase> future = new RipenFuture<AsyncTaskResultBase>();
		future.trySetResult(new AsyncTaskResult<EventAppendResult>(AsyncTaskStatus.Success, eventAppendResult));
		return future;
	}

	@Override
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(final String aggregateRootId, final int version) {
		final RipenFuture<AsyncTaskResult<DomainEventStream>> future = new RipenFuture<AsyncTaskResult<DomainEventStream>>();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Object prevous = InMemoryEventStore.this.mStorage.getOrDefault(aggregateRootId +"." +version, null);
				future.trySetResult(new AsyncTaskResult<DomainEventStream>(AsyncTaskStatus.Success, revertFromStorageFormat((String )prevous)));
			}
		}).start();
		return future;
	}

	@Override
	public Future<AsyncTaskResult<DomainEventStream>> findAsync(String aggregateRootId, String commandId) {
		return null;
	}

	@Override
	public Collection<DomainEventStream> queryAggregateEvents(String aggregateRootId, String aggregateRootTypeName,
			long minVersion, long maxVersion) {
		return null;
	}

	@Override
	public void queryAggregateEventsAsync(String aggregateRootId, String aggregateRootTypeName, long minVersion,
			long maxVersion) {
	}

	private EventAppendResult appendsync(DomainEventStream eventStream) {
		try {
			Object prevous = mStorage.putIfAbsent(eventStream.getAggregateRootId() + "." + eventStream.getVersion(),
					convertToStorageFormat(eventStream));
			if (null != prevous)
				return EventAppendResult.DuplicateEvent;
			else
				return EventAppendResult.Success;
		} catch (Exception e) {
			e.printStackTrace();
			return EventAppendResult.Failed;
		}

	}
	
	private String convertToStorageFormat(DomainEventStream eventStream) {
		return jsonConverter.convert(eventStream);
	}
	
	private DomainEventStream revertFromStorageFormat(String content) {
		return jsonConverter.revert(content, DomainEventStream.class);
	}

}