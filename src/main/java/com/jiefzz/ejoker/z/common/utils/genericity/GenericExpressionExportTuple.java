package com.jiefzz.ejoker.z.common.utils.genericity;

public class GenericExpressionExportTuple {
	
	private final GenericDeclare refDeclare;
	
	public final GenericDefinedTypeMeta declarationTypeMeta;

	public GenericExpressionExportTuple(GenericDeclare refDeclare, GenericDefinedTypeMeta declarationType) {
		super();
		this.refDeclare = refDeclare;
		this.declarationTypeMeta = declarationType;
	}
	
	public int getIndex() {
		return refDeclare.index;
	}
	
	public String getName() {
		return refDeclare.name;
	}

}
