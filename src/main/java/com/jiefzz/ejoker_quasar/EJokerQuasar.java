package com.jiefzz.ejoker_quasar;

import java.util.concurrent.TimeUnit;

import com.jiefzz.ejoker.EJoker;
import com.jiefzz.ejoker.z.common.system.wrapper.threadSleep.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.AbstractNormalWorkerGroupService;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;

/**
 * maven lifecycle org.fortasoft:gradle-maven-plugin:1.0.8:invoke 
 * @author kimffy
 *
 */
public class EJokerQuasar extends EJoker {

	public static EJoker getInstance(){
		if ( instance == null )
			instance = new EJokerQuasar();
		return instance;
	}
	
	protected EJokerQuasar() {
		super();
	}
	
	@Suspendable
	private static void sleep(TimeUnit u, Long millis) {
		try {
			Strand.sleep(millis, u);
		} catch (SuspendExecution s) {
			throw new AssertionError(s);
		}  catch (InterruptedException e) {
			// do nothing
		}
	}
	
	static {
		// 注册quasar线程入口
		AbstractNormalWorkerGroupService.setAsyncEntranceProvider(QuasarFiberExector::new);
		// 注册quasar的sleep wrapper
		SleepWrapper.setSleep(EJokerQuasar::sleep);
	}
}
