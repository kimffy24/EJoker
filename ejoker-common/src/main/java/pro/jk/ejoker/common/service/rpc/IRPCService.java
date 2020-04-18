package pro.jk.ejoker.common.service.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import pro.jk.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;

/**
 * 不使用Java对象动态代理技术。
 * <br> 框架上下文内，同意端口，就约定同一类对象调用，方式、参数类型都相同。
 */
public interface IRPCService {
	
	final static Map<Integer, RPCTuple> portMap = new HashMap<>();
	
	final static Map<Integer, AtomicBoolean> serverPortOccupation = new HashMap<>();
	
	default public void export(IVoidFunction1<String> action, int port) {
		export(action, port, false);
	}
	
	public void export(IVoidFunction1<String> action, int port, boolean waitFinished);
	
	public void remoteInvoke(String data, String host, int port);
	
	public void removeExport(int port);

	public static class RPCTuple {
		
		public final Thread ioThread;
		
		public final IVoidFunction1<String> handleAction;
		
		public final RipenFuture<Void> initialFuture;
		
		public RPCTuple(IVoidFunction1<String> handleAction, Thread ioThread) {
			this.ioThread = ioThread;
			this.handleAction = handleAction;
			this.initialFuture = new RipenFuture<>();
		}
		
	}
}
