package com.jiefzz.ejoker.z.common.utils.genericity;

import com.jiefzz.ejoker.z.common.utils.genericity.GenericDefination.GenericDefinationRef;

public class GenericDeclare extends GenericDefinationRef {
	
	public final int index;
	
	public final String name;

	public GenericDeclare(GenericDefination ref, int index, String name) {
		super(ref);
		this.index = index;
		this.name = name;
	}
	
	public boolean isSameDefination(GenericDefination ref) {
		return this.referDefination.equals(ref);
	}
	
	public boolean isNullDefination() {
		return null == this.referDefination;
	}

}
