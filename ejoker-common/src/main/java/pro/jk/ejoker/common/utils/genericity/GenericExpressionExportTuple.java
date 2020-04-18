package pro.jk.ejoker.common.utils.genericity;

public class GenericExpressionExportTuple {
	
	private final GenericDeclaration refDeclare;
	
	public final GenericDefinedType declarationTypeMeta;

	public GenericExpressionExportTuple(GenericDeclaration refDeclare, GenericDefinedType declarationType) {
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
