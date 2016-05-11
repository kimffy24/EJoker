package com.jiefzz.ejoker.domain;

import java.io.Serializable;

import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class AbstractValueObject implements Serializable {

	/**
	 * 
	 */
	@PersistentIgnore
	private static final long serialVersionUID = 1142105907868383134L;

}
