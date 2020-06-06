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
	
	default public void export(IVoidFunction1<String> action, int port) {
		export(action, port, true);
	}
	
	public void export(IVoidFunction1<String> action, int port, boolean waitFinished);
	
	public void remoteInvoke(String data, String host, int port);
	
	public void removeExport(int port);
}
