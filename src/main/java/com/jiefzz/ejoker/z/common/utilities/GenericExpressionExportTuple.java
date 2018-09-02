package com.jiefzz.ejoker.z.common.utilities;

public class GenericExpressionExportTuple {
	
	private final GenericDefinedMeta refMeta;
	
	public final GenericDefinedTypeMeta declarationTypeMeta;

	public GenericExpressionExportTuple(GenericDefinedMeta refMeta, GenericDefinedTypeMeta declarationType) {
		super();
		this.refMeta = refMeta;
		this.declarationTypeMeta = declarationType;
	}
	
	public int getIndex() {
		return refMeta.index;
	}
	
	public String getName() {
		return refMeta.name;
	}

}
