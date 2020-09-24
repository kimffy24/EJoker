package pro.jiefzz.ejoker.common.utils.relationship;

public class SData7<T> {

	private boolean success = false;
	
	private T object;
	
	private String msg;

	public boolean isSuccess() {
		return success;
	}

	public T getObject() {
		return object;
	}

	public String getMsg() {
		return msg;
	}

	@Override
	public String toString() {
		return "SData7 [success=" + success + ", object=" + object + ", msg=" + msg + "]";
	}
	
}
