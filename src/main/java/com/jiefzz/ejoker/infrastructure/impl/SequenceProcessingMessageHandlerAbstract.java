package com.jiefzz.ejoker.infrastructure.impl;

import java.io.IOException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IPublishedVersionStore;
import com.jiefzz.ejoker.infrastructure.ISequenceMessage;
import com.jiefzz.ejoker.infrastructure.ISequenceProcessingMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

public abstract class SequenceProcessingMessageHandlerAbstract<X extends IProcessingMessage<X, Y> & ISequenceProcessingMessage , Y extends ISequenceMessage>
		implements IProcessingMessageHandler<X, Y> {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Dependence
	private IPublishedVersionStore publishedVersionStore;

	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public void handleAsync(X processingMessage) {
		systemAsyncHelper.submit(() -> handleMessageAsync(processingMessage));
		processingMessage.complete();
	}
	
	public abstract String getName();

    protected abstract Future<AsyncTaskResultBase> dispatchProcessingMessageAsync(X processingMessage);
    
    private void handleMessageAsync(X processingMessage) {
    	
        Y message = processingMessage.getMessage();
        
        ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<Long>>() {

			@Override
			public String getAsyncActionName() {
				return "GetPublishedVersionAsync";
			}

			@Override
			public Future<AsyncTaskResult<Long>> asyncAction() throws IOException {
				return publishedVersionStore.getPublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId());
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
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
    	
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResultBase>() {

			@Override
			public String getAsyncActionName() {
				return "DispatchProcessingMessageAsync";
			}

			@Override
			public Future<AsyncTaskResultBase> asyncAction() throws IOException {
				return dispatchProcessingMessageAsync(processingMessage);
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
			}

			@Override
			public void finishAction(AsyncTaskResultBase result) {
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
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResultBase>() {

			@Override
			public String getAsyncActionName() {
				return "UpdatePublishedVersionAsync";
			}

			@Override
			public Future<AsyncTaskResultBase> asyncAction() throws IOException {
				Y message = processingMessage.getMessage();
				return publishedVersionStore.updatePublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId(), message.getVersion());
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
			}

			@Override
			public void finishAction(AsyncTaskResultBase result) {
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
