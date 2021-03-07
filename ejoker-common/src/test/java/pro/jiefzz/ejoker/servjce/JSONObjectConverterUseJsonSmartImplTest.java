package pro.jiefzz.ejoker.servjce;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONObject;
import pro.jiefzz.ejoker.common.utils.relationship.SData1;
import pro.jiefzz.ejoker.common.utils.relationship.SData2;
import pro.jiefzz.ejoker.common.utils.relationship.SData7;
import pro.jk.ejoker.common.service.impl.JSONObjectConverterUseJsonSmartImpl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class JSONObjectConverterUseJsonSmartImplTest {
	
	private final static Logger logger = LoggerFactory.getLogger(JSONObjectConverterUseJsonSmartImplTest.class);

	JSONObjectConverterUseJsonSmartImpl worker = new JSONObjectConverterUseJsonSmartImpl();
	
	@Test
	public void test1() {
		SData1 sData1 = new SData1(4.5, 4.7f, 5l, Byte.MAX_VALUE, Integer.MAX_VALUE - 1, (short )7);
		logger.info("target: {}", sData1);
		JSONObject convert = worker.convert(sData1);
		logger.info("convert: {}", convert);
		Object revert = worker.revert(convert, SData1.class);
		logger.info("revert: {}", revert);
		SData1 revert2 = worker.revert(convert, new TypeRefer<SData1>() { });
		logger.info("revert: {}", revert2);

		Object revert3 = worker.revert(convert, SData2.class);
		logger.info("revert: {}", revert3);
		SData2 revert4 = worker.revert(convert, new TypeRefer<SData2>() { });
		logger.info("revert: {}", revert4);
	}

	@Test
	public void test2() {
		String testStr = "{\"success\": true, \"object\": 123, \"msg\": \"ok\"}";
		SData7<Long> sd7 = new SData7<>();
		worker.convert(sd7, new TypeRefer<SData7<Long>>() { });
		
	}
}
