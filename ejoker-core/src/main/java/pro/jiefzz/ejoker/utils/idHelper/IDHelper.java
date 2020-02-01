package pro.jiefzz.ejoker.utils.idHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.system.helper.Ensure;
import pro.jiefzz.ejoker.common.system.helper.StringHelper;
import pro.jiefzz.ejoker.common.utils.genericity.GenericDefinedField;
import pro.jiefzz.ejoker.common.utils.genericity.GenericExpression;
import pro.jiefzz.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jiefzz.ejoker.domain.IAggregateRoot;

public final class IDHelper {
	
	private final static Logger logger = LoggerFactory.getLogger(IDHelper.class);
	
	private final static Map<Class<?>, GenericDefinedField> idGdcCache = new HashMap<>();

	private final static Map<Class<?>, IDCodec<?>> codecStore = new HashMap<>();
	
	public static <S> void applyCodec(Class<S> type, IDCodec<S> codec) {
		if(null != codecStore.putIfAbsent(type, codec)) {
			logger.error("Cannot replace Aggregate root Id codec, current codec for: {}", type.getName());
		}
	}
	
	public static void addAggregateRoot(Class<IAggregateRoot> aggrType) {
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(aggrType);
		
		final AtomicBoolean ok = new AtomicBoolean(false);
		genericExpress.forEachFieldExpressionsDeeply((fieldName, fieldTypeDef) -> {
			if(!ok.get()) {
				if("id".equals(fieldName)) {

					Ensure.notNull(fieldTypeDef.genericDefinedType, "GenericDefinedField.genericDefinedTypeMeta");
					Ensure.notNull(fieldTypeDef.genericDefinedType.rawClazz, "GenericDefinedField.genericDefinedTypeMeta.rawClazz");
					
					ok.set(true);
					idGdcCache.put(aggrType, fieldTypeDef);
				}
			}
		});
	}

	public static void setAggregateRootId(IAggregateRoot aggr, String stringId) {
		GenericDefinedField gdf;
		if (null == (gdf = idGdcCache.get(aggr.getClass())))
			throw new RuntimeException(StringHelper.fill("Type defined is not found!!! [aggregateRootType: {}]", aggr.getClass().getName()));

		Object decode = codecStore.get(gdf.genericDefinedType.rawClazz).decode(stringId);
		try {
			gdf.field.set(aggr, decode);
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			ex.printStackTrace();
			throw new RuntimeException(
					StringHelper.fill("Faild to set id to AggregateRoot!!! [aggregateType: {}, idValue: {}]",
							aggr.getClass().getName(), stringId),
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
