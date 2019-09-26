package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public interface IProcessingCommandHandler {

	public SystemFutureWrapper<Void> handle(ProcessingCommand processingCommand);
	
}
