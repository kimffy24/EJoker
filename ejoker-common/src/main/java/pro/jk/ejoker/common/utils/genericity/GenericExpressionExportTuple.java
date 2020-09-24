package pro.jk.ejoker.common.utils.genericity;

/**
 * 用于记录泛型声明在具现化表达时，从下级继承类或扩展类的具现化向上传递的关系。<br />
 * 例如<br />
 * 有一接口 定义为 IService&lt;T&gt; <br />
 * 在某一地点有声明一实例<br />
 * <br />class ServiceA implements IService&lt;Integer&gt; {
 * <br />	//...
 * <br />}
 * <br />
 * <br />那么构建GenericExpression时，将会构建一个GenericExpressionExportTuple
 * <br />其中 GenericDeclaration 描述的是 泛型变量T的位置和名字
 * <br />GenericDefinedType 描述的是声明的IService&lt;Integer&gt;中的Integer部分相关的信息
 * 
 * @author kimffy
 *
 */
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
