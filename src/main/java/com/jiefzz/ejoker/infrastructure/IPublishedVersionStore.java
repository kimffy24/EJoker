package com.jiefzz.ejoker.infrastructure;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public interface IPublishedVersionStore {
	
	/**
	 * Update the published version for the given aggregate.
	 * @param processorName
	 * @param aggregateRootTypeName
	 * @param aggregateRootId
	 * @param publishedVersion
	 * @return
	 */
    Future<AsyncTaskResultBase> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, long publishedVersion);
    
    /**
     * Get the current published version for the given aggregate.
     * @param processorName
     * @param aggregateRootTypeName
     * @param aggregateRootId
     * @return
     */
    Future<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId);
    
    
}
