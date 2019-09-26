package pro.jiefzz.ejoker.utils.idHelper;

public interface IDCodec<T> {

	public String encode(T source);
	
	public T decode(String dist);
	
}
