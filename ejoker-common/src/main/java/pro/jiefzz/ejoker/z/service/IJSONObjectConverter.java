package pro.jiefzz.ejoker.z.service;

import net.minidev.json.JSONObject;

public interface IJSONObjectConverter {

	public <T> JSONObject convert(T object);
	
	public <T> T revert(JSONObject jsonObject, Class<T> clazz);
	
}
