package com.jiefzz.ejoker;

import com.jiefzz.ejoker.z.common.context.IEJokerContext;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.context.impl.DefaultEJokerContext;

/**
 * E-Joker instance provider. E-Joker context provider.
 * @author JiefzzLon
 *
 */
public class EJoker {
	
	//////  public:
	
	public static final String SELF_PACNAGE_NAME = "com.jiefzz.ejoker";
	
	public static EJoker getInstance(){
		if ( instance == null )
			instance = new EJoker();
		return instance;
	}
	
	public IEJokerSimpleContext getEJokerContext(){
		return context;
	}

	//////  private:
	
	private EJoker() {
		context = new DefaultEJokerContext();
		context.scanPackageClassMeta(SELF_PACNAGE_NAME);
	}

	//////  properties:
	
	private static EJoker instance;
	private IEJokerContext context;
	
}
