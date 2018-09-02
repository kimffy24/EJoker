package com.jiefzz.ejoker.z.common.utilities;

import com.jiefzz.ejoker.z.common.utilities.GenericDefination.GenericDefinationRef;

public class GenericDefinedMeta extends GenericDefinationRef {
	
	public final int index;
	
	public final String name;

	public GenericDefinedMeta(GenericDefination ref, int index, String name) {
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
