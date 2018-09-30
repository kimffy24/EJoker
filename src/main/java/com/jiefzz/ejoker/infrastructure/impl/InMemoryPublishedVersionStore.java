package com.jiefzz.ejoker.infrastructure.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.infrastructure.IPublishedVersionStore;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

@EService
public class InMemoryPublishedVersionStore implements IPublishedVersionStore {

	private final Future<AsyncTaskResultBase> successTask;

	private final Map<String, Long> versionDict = new ConcurrentHashMap<>();

	public InMemoryPublishedVersionStore() {
		successTask = new RipenFuture<AsyncTaskResultBase>();
		((RipenFuture<AsyncTaskResultBase>) successTask).trySetResult(AsyncTaskResultBase.Success);
	}

	@Override
	public Future<AsyncTaskResultBase> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId, long publishedVersion) {
		versionDict.put(buildKey(processorName, aggregateRootId), publishedVersion);
		return successTask;
	}

	@Override
	public Future<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId) {
		Long version = versionDict.getOrDefault(buildKey(processorName, aggregateRootId), 0l);

		AsyncTaskResult<Long> taskResult = new AsyncTaskResult<>(AsyncTaskStatus.Success, version);

		RipenFuture<AsyncTaskResult<Long>> result = new RipenFuture<>();
		result.trySetResult(taskResult);

		return result;
	}

	private String buildKey(String eventProcessorName, String aggregateRootId) {
		return String.format("%s-%s", eventProcessorName, aggregateRootId);
	}

}
