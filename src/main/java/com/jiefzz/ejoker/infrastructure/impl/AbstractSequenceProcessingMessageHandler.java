package com.jiefzz.ejoker.infrastructure.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IPublishedVersionStore;
import com.jiefzz.ejoker.infrastructure.ISequenceMessage;
import com.jiefzz.ejoker.infrastructure.ISequenceProcessingMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

public abstract class AbstractSequenceProcessingMessageHandler<X extends IProcessingMessage<X, Y> & ISequenceProcessingMessage , Y extends ISequenceMessage>
		implements IProcessingMessageHandler<X, Y> {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Dependence
	private IPublishedVersionStore publishedVersionStore;

	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	public abstract String getName();

    protected abstract SystemFutureWrapper<AsyncTaskResult<Void>> dispatchProcessingMessageAsync(X processingMessage);

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage) {
		return eJokerAsyncHelper.submit(() -> handleMessageAsync(processingMessage));
	}

    private void handleMessageAsync(X processingMessage) {
    	
        Y message = processingMessage.getMessage();
        IFunction<String> contextInfo = () -> String.format(
				"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
				message.getId(),
				message.getClass().getName(),
				message.getAggregateRootStringId(),
				message.getVersion());
        ioHelper.tryAsyncAction2(
        		"GetPublishedVersionAsync",
        		() -> publishedVersionStore.getPublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId()),
        		r -> {
    				long publishedVersion = r.longValue();
    				long currentEventVersion = message.getVersion();
    				if (publishedVersion + 1l - currentEventVersion == 0l) {
    					dispatchProcessingMessageAsyncInternal(processingMessage);
    				} else if (publishedVersion + 1l - currentEventVersion < 0l) {
    					logger.warn(
    							"The sequence message cannot be process now as the version is not the next version, it will be handle later! contextInfo [aggregateRootId={}, lastPublishedVersion={}, messageVersion={}]",
    							message.getAggregateRootStringId(), publishedVersion, currentEventVersion);
    					processingMessage.addToWaitingList();
    				} else {
    					logger.warn("This sequence message has been processed before! contextInfo: {}", contextInfo.trigger());
    					processingMessage.complete();
    				}},
        		contextInfo,
        		ex -> logger.error("Get published version has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage()),
        		true
        		);
        
    }
    
    private void dispatchProcessingMessageAsyncInternal(X processingMessage) {

		Y message = processingMessage.getMessage();
    	ioHelper.tryAsyncAction2(
    			"DispatchProcessingMessageAsync",
    			() -> dispatchProcessingMessageAsync(processingMessage),
    			r -> updatePublishedVersionAsync(processingMessage),
    			() -> String.format(
    					"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
    					message.getId(),
    					message.getClass().getName(),
    					message.getAggregateRootStringId(),
    					message.getVersion()),
    			ex -> logger.error(String.format(
						"Dispatching message has unknown exception, the code should not be run to here, errorMessage: %s",
						ex.getMessage()),
					ex),
    			true
    			);
    }
    
    private void updatePublishedVersionAsync(X processingMessage) {
    	
		Y message = processingMessage.getMessage();
		ioHelper.tryAsyncAction2(
				"UpdatePublishedVersionAsync",
				() -> publishedVersionStore.updatePublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId(), message.getVersion()),
				r -> processingMessage.complete(),
				() -> String.format(
						"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
						message.getId(),
						message.getClass().getName(),
						message.getAggregateRootStringId(),
						message.getVersion()),
				ex -> logger.error(String.format("Update published version has unknown exception, the code should not be run to here, errorMessage: %s", ex.getMessage())),
				true
				)
		;
    }
    
}
