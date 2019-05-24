package com.jiefzz.ejoker.z.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IJSONObjectConverter;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;

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
