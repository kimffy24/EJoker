package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface IPublishedVersionStore {
	
	/**
	 * Update the published version for the given aggregate.
	 * @param processorName
	 * @param aggregateRootTypeName
	 * @param aggregateRootId
	 * @param publishedVersion
	 * @return
	 */
	SystemFutureWrapper<AsyncTaskResult<Void>> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, long publishedVersion);
    
    /**
     * Get the current published version for the given aggregate.
     * @param processorName
     * @param aggregateRootTypeName
     * @param aggregateRootId
     * @return
     */
	SystemFutureWrapper<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId);

}
