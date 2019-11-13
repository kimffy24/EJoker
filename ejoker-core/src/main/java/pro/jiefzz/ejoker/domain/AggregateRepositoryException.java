package pro.jiefzz.ejoker.domain;

public class AggregateRepositoryException extends RuntimeException {

	private static final long serialVersionUID = 872601114977415812L;

	public AggregateRepositoryException(String msg){
		super(msg);
	}
	
	public AggregateRepositoryException(String msg, RuntimeException e){
		super(msg, e);
	}
	
}
