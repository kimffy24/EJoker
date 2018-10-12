package com.jiefzz.ejoker.infrastructure.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jiefzz.ejoker.infrastructure.IPublishedVersionStore;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.EJokerFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

//@EService
public class InMemoryPublishedVersionStore implements IPublishedVersionStore {

	private final SystemFutureWrapper<AsyncTaskResult<Void>> successTask
		= EJokerFutureWrapperUtil.createCompleteFutureTask();

	private final Map<String, Long> versionDict = new ConcurrentHashMap<>();

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId, long publishedVersion) {
		updatePublishedVersion(processorName, aggregateRootTypeName, aggregateRootId, publishedVersion);
		return successTask;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId) {
		return EJokerFutureWrapperUtil.createCompleteFutureTask(getPublishedVersion(processorName, aggregateRootTypeName, aggregateRootId));
		
	}

	@Override
	public void updatePublishedVersion(String processorName, String aggregateRootTypeName, String aggregateRootId,
			long publishedVersion) {
		versionDict.put(buildKey(processorName, aggregateRootId), publishedVersion);
	}

	@Override
	public long getPublishedVersion(String processorName, String aggregateRootTypeName, String aggregateRootId) {
		return versionDict.getOrDefault(buildKey(processorName, aggregateRootId), 0l);
	}

	private String buildKey(String eventProcessorName, String aggregateRootId) {
		return String.format("%s-%s", eventProcessorName, aggregateRootId);
	}
	
}
