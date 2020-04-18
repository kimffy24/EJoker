package pro.jk.ejoker.bootstrap;

import pro.jk.ejoker_support.equasar.EJoker;

public class EJokerQuasarBootstrap extends EJokerBootstrap {

	public EJokerQuasarBootstrap(String... packages) /*throws SuspendExecution */{
		super(() -> EJoker.class, packages);
	}
	
}
