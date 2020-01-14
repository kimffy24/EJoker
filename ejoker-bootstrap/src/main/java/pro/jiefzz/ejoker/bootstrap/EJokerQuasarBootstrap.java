package pro.jiefzz.ejoker.bootstrap;

import pro.jiefzz.ejoker_support.equasar.EJoker;

public class EJokerQuasarBootstrap extends EJokerBootstrap {

	public EJokerQuasarBootstrap(String... packages) /*throws SuspendExecution */{
		super(() -> EJoker.class, packages);
	}
	
}
