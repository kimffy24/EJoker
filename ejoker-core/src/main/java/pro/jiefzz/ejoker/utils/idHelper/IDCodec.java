package pro.jiefzz.ejoker.utils.idHelper;

public interface IDCodec<T> {

	/**
	 * encode the generic type to string
	 * @param source
	 * @return
	 */
	public String encode(T source);
	
	/**
	 * decode the string to generic type
	 * @param dist
	 * @return
	 */
	public T decode(String dist);
	
}
