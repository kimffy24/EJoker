package com.jiefzz.ejoker.utils.idHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.z.common.utils.Ensure;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericDefinedField;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpression;
import com.jiefzz.ejoker.z.common.utils.genericity.GenericExpressionFactory;

public final class IDHelper {
	
	private final static Logger logger = LoggerFactory.getLogger(IDHelper.class);
	
	private final static Map<Class<?>, GenericDefinedField> idGdcCache = new HashMap<>();

	private final static Map<Class<?>, IDCodec<?>> codecStore = new HashMap<>();
	
	public static <S> void applyCodec(Class<S> type, IDCodec<S> codec) {
		if(null != codecStore.putIfAbsent(type, codec)) {
			logger.error("Cannot replace Aggregate root Id codec, current codec: {}", codec.getClass().getName());
		}
	}
	
	public static void addAggregateRoot(Class<IAggregateRoot> aggrType) {
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(aggrType);
		
		final AtomicBoolean ok = new AtomicBoolean(false);
		genericExpress.forEachFieldExpressionsDeeply((fieldName, fieldTypeDef) -> {
			if(!ok.get()) {
				if("id".equals(fieldName)) {

					Ensure.notNull(fieldTypeDef.genericDefinedTypeMeta, "GenericDefinedField.genericDefinedTypeMeta");
					Ensure.notNull(fieldTypeDef.genericDefinedTypeMeta.rawClazz, "GenericDefinedField.genericDefinedTypeMeta.rawClazz");
					
					ok.set(true);
					idGdcCache.put(aggrType, fieldTypeDef);
				}
			}
		});
	}

	public static void setAggregateRootId(IAggregateRoot aggr, String stringId) {
		GenericDefinedField gdf;
		if (null == (gdf = idGdcCache.get(aggr.getClass())))
			throw new RuntimeException(String.format("Type defined for %s is not found!!!", aggr.getClass()));

		Object decode = codecStore.get(gdf.genericDefinedTypeMeta.rawClazz).decode(stringId);
		try {
			gdf.field.set(aggr, decode);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			ex.printStackTrace();
			throw new RuntimeException(
					String.format("Set value[value=%s] to field[fieldName=%s, aggregate=%s is faild!!!", stringId, "id",
							aggr.getClass()),
					ex);
		}
	}
	
	static {

		// 这个是为了统一行为
		applyCodec(String.class, new IDCodec<String>() {
			@Override
			public String encode(String source) {
				return source;
			}
			@Override
			public String decode(String dist) {
				return dist;
			}
		});
		
		applyCodec(Integer.class, new IDCodec<Integer>() {
			@Override
			public String encode(Integer source) {
				return source.toString();
			}
			@Override
			public Integer decode(String dist) {
				return Integer.valueOf(dist);
			}
		});

		applyCodec(Long.class, new IDCodec<Long>() {
			@Override
			public String encode(Long source) {
				return source.toString();
			}
			@Override
			public Long decode(String dist) {
				return Long.valueOf(dist);
			}
		});

		applyCodec(Short.class, new IDCodec<Short>() {
			@Override
			public String encode(Short source) {
				return source.toString();
			}
			@Override
			public Short decode(String dist) {
				return Short.valueOf(dist);
			}
		});

	}
	
}
