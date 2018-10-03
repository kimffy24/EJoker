package com.jiefzz.ejoker.infrastructure.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IPublishedVersionStore;
import com.jiefzz.ejoker.infrastructure.ISequenceMessage;
import com.jiefzz.ejoker.infrastructure.ISequenceProcessingMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;

public abstract class SequenceProcessingMessageHandlerA<X extends IProcessingMessage<X, Y> & ISequenceProcessingMessage , Y extends ISequenceMessage>
		implements IProcessingMessageHandler<X, Y> {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Dependence
	private IPublishedVersionStore publishedVersionStore;

	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private EJokerAsyncHelper eJokerAsyncHelper;
	
	public abstract String getName();

    protected abstract SystemFutureWrapper<AsyncTaskResult<Void>> dispatchProcessingMessageAsync(X processingMessage);

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(X processingMessage) {
		return eJokerAsyncHelper.submit(() -> handleMessageAsync(processingMessage));
	}
	
    private void handleMessageAsync(X processingMessage) {
    	
        Y message = processingMessage.getMessage();
        
        ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<Long>>() {

			@Override
			public String getAsyncActionName() {
				return "GetPublishedVersionAsync";
			}

			@Override
			public AsyncTaskResult<Long> asyncAction() throws Exception {
				return publishedVersionStore.getPublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId()).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<Long> result) {
				long publishedVersion = result.getData();
				long currentEventVersion = message.getVersion();
				if (publishedVersion + 1 == currentEventVersion) {
					dispatchProcessingMessageAsyncInternal(processingMessage);
				} else if (publishedVersion + 1 < currentEventVersion) {
					logger.debug(
							"The sequence message cannot be process now as the version is not the next version, it will be handle later. contextInfo [aggregateRootId={},lastPublishedVersion={},messageVersion={}]",
							message.getAggregateRootStringId(), publishedVersion, currentEventVersion);
					processingMessage.addToWaitingList();
				} else {
					processingMessage.complete();
				}
			}

			@Override
			public void faildAction(Exception ex) {
				ex.printStackTrace();
				logger.error("Get published version has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage());
			}

			@Override
			public String getContextInfo() {
				return String.format(
						"sequence message [messageId:{}, messageType:{}, aggregateRootId:{}, aggregateRootVersion:{}]",
						message.getId(), message.getClass().getName(), message.getAggregateRootStringId(),
						message.getVersion());
			}
        	
        });
        
    }
    
    private void dispatchProcessingMessageAsyncInternal(X processingMessage) {
    	
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<Void>>() {

			@Override
			public String getAsyncActionName() {
				return "DispatchProcessingMessageAsync";
			}

			@Override
			public AsyncTaskResult<Void> asyncAction() throws Exception {
				return dispatchProcessingMessageAsync(processingMessage).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<Void> result) {
				updatePublishedVersionAsync(processingMessage);
			}

			@Override
			public void faildAction(Exception ex) {
				ex.printStackTrace();
				logger.error(
						"Dispatching message has unknown exception, the code should not be run to here, errorMessage: {}",
						ex.getMessage());
			}

			@Override
			public String getContextInfo() {
				Y message = processingMessage.getMessage();
				return String.format(
						"sequence message [messageId:{}, messageType:{}, aggregateRootId:{}, aggregateRootVersion:{}]",
						message.getId(), message.getClass().getName(), message.getAggregateRootStringId(),
						message.getVersion());
			}
			
    		
    	});
    	
    }
    private void updatePublishedVersionAsync(X processingMessage) {
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<Void>>() {

			@Override
			public String getAsyncActionName() {
				return "UpdatePublishedVersionAsync";
			}

			@Override
			public AsyncTaskResult<Void> asyncAction() throws Exception {
				Y message = processingMessage.getMessage();
				return publishedVersionStore.updatePublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId(), message.getVersion()).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<Void> result) {
				processingMessage.complete();
			}

			@Override
			public void faildAction(Exception ex) {
				ex.printStackTrace();
				logger.error("Update published version has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage());
			}
			
			@Override
			public String getContextInfo() {
				Y message = processingMessage.getMessage();
				return String.format(
						"sequence message [messageId:{}, messageType:{}, aggregateRootId:{}, aggregateRootVersion:{}]",
						message.getId(), message.getClass().getName(), message.getAggregateRootStringId(),
						message.getVersion());
			}
			
		});
    	
    }
    
}
