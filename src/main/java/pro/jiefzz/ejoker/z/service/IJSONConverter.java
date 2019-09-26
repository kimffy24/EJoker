package pro.jiefzz.ejoker.z.service;

public interface IJSONConverter {

	public <T> String convert(T object);
	
	public <T> T revert(String jsonString, Class<T> clazz);
	
}
