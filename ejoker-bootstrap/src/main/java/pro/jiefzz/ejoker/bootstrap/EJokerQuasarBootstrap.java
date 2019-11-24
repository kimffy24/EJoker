package pro.jiefzz.ejoker.bootstrap;

import co.paralleluniverse.fibers.SuspendExecution;
import pro.jiefzz.ejoker_support.equasar.EJoker;

public class EJokerQuasarBootstrap extends EJokerBootstrap {

	public EJokerQuasarBootstrap(String... packages) throws SuspendExecution {
		// 这个类是 pro.jiefzz.equasar.EJoker
		// 而不是 pro.jiefzz.ejoker.EJoker
		super(() -> EJoker.class, packages);
	}
	
}
