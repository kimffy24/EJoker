package pro.jiefzz.ejoker.z.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import pro.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.service.IJSONObjectConverter;

@EService
public class JSONConverterUseJsonSmartImpl implements IJSONConverter {

	private final static Logger logger = LoggerFactory.getLogger(JSONConverterUseJsonSmartImpl.class);

	@Dependence
	private IJSONObjectConverter jsonObjectConverter;
	
	@Override
	public <T> String convert(T object) {
		JSONObject result = jsonObjectConverter.convert(object);
		return JSONValue.toJSONString(result, JSONStyle.NO_COMPRESS);
	}

	@Override
	public <T> T revert(String jsonString, Class<T> clazz) {
		try {
			return jsonObjectConverter.revert((JSONObject )JSONValue.parseStrict(jsonString), clazz);
		} catch (ParseException e) {
			logger.error("revert JsonObject failed!!!", e);
			throw new InfrastructureRuntimeException("revert JsonObject failed!!!", e);
		}
	}

}
