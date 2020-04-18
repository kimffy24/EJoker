package pro.jk.ejoker.infrastructure;

public class InfrastructureRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 6189272934620408965L;

	public InfrastructureRuntimeException(String msg){
		super(msg);
	}

	public InfrastructureRuntimeException(String msg, Exception e){
		super(msg, e);
	}
}
