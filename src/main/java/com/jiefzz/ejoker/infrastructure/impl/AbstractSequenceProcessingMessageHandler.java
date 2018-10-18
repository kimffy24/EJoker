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
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

import co.paralleluniverse.fibers.SuspendExecution;

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
		return eJokerAsyncHelper.submit(() -> handle(processingMessage));
	}

	@Override
	public void handle(X processingMessage) {
		handleMessageAsync(processingMessage);
	}
	
    private void handleMessageAsync(X processingMessage) {
    	
        Y message = processingMessage.getMessage();
        ioHelper.tryAsyncAction(new IOActionExecutionContext<Long>(true) {

			@Override
			public String getAsyncActionName() {
				return "GetPublishedVersionAsync";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<Long>> asyncAction() throws SuspendExecution {
				return publishedVersionStore.getPublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId());
			}

			@Override
			public void finishAction(Long result) throws SuspendExecution {
				long publishedVersion = result.longValue();
				long currentEventVersion = message.getVersion();
				if (publishedVersion + 1l - currentEventVersion == 0l) {
					dispatchProcessingMessageAsyncInternal(processingMessage);
				} else if (publishedVersion + 1l - currentEventVersion < 0l) {
					logger.warn(
							"The sequence message cannot be process now as the version is not the next version, it will be handle later! contextInfo [aggregateRootId={}, lastPublishedVersion={}, messageVersion={}]",
							message.getAggregateRootStringId(), publishedVersion, currentEventVersion);
					processingMessage.addToWaitingList();
				} else {
					logger.warn("This sequence message has been processed before! contextInfo: {}", this.getContextInfo());
					processingMessage.complete();
				}
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				ex.printStackTrace();
				logger.error("Get published version has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage());
			}

			@Override
			public String getContextInfo() {
				return String.format(
						"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
						message.getId(), message.getClass().getName(), message.getAggregateRootStringId(),
						message.getVersion());
			}
        	
        });
        
    }
    
    private void dispatchProcessingMessageAsyncInternal(X processingMessage) {
    	
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<Void>(true) {

			@Override
			public String getAsyncActionName() {
				return "DispatchProcessingMessageAsync";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<Void>> asyncAction() throws SuspendExecution {
				return dispatchProcessingMessageAsync(processingMessage);
			}

			@Override
			public void finishAction(Void result) throws SuspendExecution {
				updatePublishedVersionAsync(processingMessage);
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				logger.error(String.format(
							"Dispatching message has unknown exception, the code should not be run to here, errorMessage: %s",
							ex.getMessage()),
						ex);
			}

			@Override
			public String getContextInfo() {
				Y message = processingMessage.getMessage();
				return String.format(
						"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
						message.getId(), message.getClass().getName(), message.getAggregateRootStringId(),
						message.getVersion());
			}
			
    		
    	});
    	
    }
    private void updatePublishedVersionAsync(X processingMessage) {
    	
		Y message = processingMessage.getMessage();
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<Void>(true) {

			@Override
			public String getAsyncActionName() {
				return "UpdatePublishedVersionAsync";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<Void>> asyncAction() throws SuspendExecution {
				return publishedVersionStore.updatePublishedVersionAsync(getName(), message.getAggregateRootTypeName(), message.getAggregateRootStringId(), message.getVersion());
			}

			@Override
			public void finishAction(Void result) throws SuspendExecution {
				processingMessage.complete();
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				logger.error(String.format("Update published version has unknown exception, the code should not be run to here, errorMessage: %s", ex.getMessage()), ex);
			}
			
			@Override
			public String getContextInfo() {
				return String.format(
						"sequence message [messageId:%s, messageType:%s, aggregateRootId:%s, aggregateRootVersion:%d]",
						message.getId(), message.getClass().getName(), message.getAggregateRootStringId(),
						message.getVersion());
			}
			
		});
    	
    }
    
}
