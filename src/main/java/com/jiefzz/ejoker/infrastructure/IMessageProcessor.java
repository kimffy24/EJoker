package com.jiefzz.ejoker.infrastructure;

public interface IMessageProcessor<X extends IProcessingMessage<X, Y>, Y extends IMessage> {

	void process(X processingMessage);
	
}
