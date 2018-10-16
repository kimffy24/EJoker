package com.jiefzz.ejoker_quasar_test;

import com.jiefzz.ejoker_quasar.EJokerQuasarBootstrap;

public class QEJokerTest {

	public static void main(String[] args) throws Exception {
		
		long s = System.currentTimeMillis();
		
		EJokerQuasarBootstrap bootstrap = new EJokerQuasarBootstrap("com.jiefzz.ejoker_quasar_test");
		
		bootstrap.initDomainEventConsumer();
		bootstrap.initDomainEventPublisher();
		bootstrap.initCommandConsumer();
		bootstrap.initCommandService();
		bootstrap.initCommandResultProcessor();
		
		bootstrap.discard();
		System.err.println("use: " + (System.currentTimeMillis() -s) + "ms");
	}
}
