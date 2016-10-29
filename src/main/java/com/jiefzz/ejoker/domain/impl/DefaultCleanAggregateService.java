package com.jiefzz.ejoker.domain.impl;

import com.jiefzz.ejoker.domain.ICleanAggregateService;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * TODO 未完成
 * @author jiefzz
 *
 */
@EService
public class DefaultCleanAggregateService implements ICleanAggregateService {
	
	@Dependence
	IMemoryCache memortCache;
	
	private final int timeoutSeconds = 1800;
	
	
	@Override
	public void clean() {
		throw new UnimplementException(this.getClass().getName() +"#clean()");
	}

}
