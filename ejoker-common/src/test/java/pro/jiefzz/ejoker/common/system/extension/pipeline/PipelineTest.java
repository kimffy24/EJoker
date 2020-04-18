package pro.jiefzz.ejoker.common.system.extension.pipeline;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.extension.pipeline.Pipeline;
import pro.jk.ejoker.common.system.extension.pipeline.PipelineHook;

public class PipelineTest {

//	@Test
	public void testSample() {

		Runnable p = new Pipeline<>(() -> {
			return new Date();
		}).add(d -> {
			return "Today is " + d.toString();
		}).add(s -> {
			System.err.println(s);
		});

		p.run();

	}

	public List<String> fetchFromApi() {
		List<String> names = new ArrayList<>();
		names.add("大炮");
		names.add("车干");
		names.add("牛逼飞");
		return names;
	}
	
	public Map<String, String> doCalculateOrStatistics(List<String> dataFromApi) {
		Map<String, String> res = new HashMap<>();
		dataFromApi.forEach(s -> res.put(s, StringUtilx.fmt("Hello {}, today is '{}', have a nice day~", s, new Date().toString())));
		return res;
	}
	
	public void doEffect(Map<String, String> serviceResult) {
		serviceResult.forEach((k, v) -> System.err.println(v));
	}

//	@Test
	public void testSample2() {

		Runnable p = new Pipeline<>(this::fetchFromApi)
				.add(this::doCalculateOrStatistics)
				.add(this::doEffect);

		p.run();

	}
	
	private Map<String, String> fetchFromDB() {
		Map<String, String> decs = new HashMap<>();
		decs.put("大炮", "爷");
		decs.put("车干", "你大爷");
		decs.put("金飞", "牛逼的大爷");
		decs.put("C君", "烦人的主");
		decs.put("寿司", "氪金兽");
		decs.put("他", "千古X君");
		return decs;
	}
	
	private Map<String, String> filter(Map<String, String> src, int match, int split) {
		Iterator<Entry<String, String>> iterator = src.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, String> curr = iterator.next();
			int hashCode = curr.getKey().hashCode();
			if(hashCode < 0)
				hashCode *= -1;
			if(hashCode % split != match)
				iterator.remove();
		}
		return src;
	}
	
	public List<String> doSth(Map<String, String> src) {
		List<String> res = new LinkedList<>();
		src.forEach((k, v) -> res.add(StringUtilx.fmt("Hello {}, u r {}, have a nice day~", k, v)));
		return res;
	}

	@Test
	public void testInExecutor2() throws InterruptedException {
		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
		((ThreadPoolExecutor) fixedThreadPool).prestartAllCoreThreads();

		int i=0;
//		for(int i=0; i<3; i++)
			fixedThreadPool.execute(new Pipeline<>(this::fetchFromDB)
					.add(this::filter, i, 3)
					.add(this::doSth)
					.add(s -> { s.forEach(System.err::println); })
					);
		
		fixedThreadPool.shutdown();
		fixedThreadPool.awaitTermination(10000, TimeUnit.MILLISECONDS);
	}
	
	private String fixName(String src) {
		if("他".equals(src))
			return src;
		return "ll" + src;
	}
	
	private String buildGoods(String name, String desc, AtomicInteger ai) {
		return StringUtilx.fmt("Curr index: {}, Today is {}, {} did great! and he/she is {}", ai.getAndIncrement(), new Date(), name, desc);
	}

//	@Test
	public void testInExecutor3() throws InterruptedException {

		Map<String, String> decs = new HashMap<>();
		decs.put("大炮", "爷");
		decs.put("车干", "你大爷");
		decs.put("金飞", "牛逼的大爷");
		decs.put("C君", "烦人的主");
		decs.put("寿司", "氪金兽");
		decs.put("他", "千古X君");
		
//		PipelineHook hook = new PipelineHook((cxt, ex, args) -> {
//			System.err.println(StringHelper.fill("{} [{}]", ex.getMessage(), getCxtInfo(args)));
//			ex.printStackTrace();
//			cxt.setPreventThrow();
//			});
//		.addPipelineHook(hook)

		AtomicInteger ai = new AtomicInteger(); // 给定要给外部参数用于测试。
		decs.forEach((k, v) -> {
			new Pipeline<>(this::fixName, k)
					.add(this::buildGoods, v, ai)
					.add(this::decorate, (new Date()).toString(), System.getenv("HOME"), System.currentTimeMillis(), Math.PI, (System.currentTimeMillis() % 2 == 0))
					.add(s -> { System.err.println(s); })
					.run()
					;
		});

	}

//	@Test
	public void testInExecutor4() throws InterruptedException {

		ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
		((ThreadPoolExecutor) fixedThreadPool).prestartAllCoreThreads();

		@SuppressWarnings("serial")
		Map<String, String> decs = new HashMap<String, String>() {
			{
				put("大炮", "爷");
				put("车干", "你大爷");
				put("金飞", "牛逼的大爷");
				put("C君", "烦人的主");
				put("寿司", "氪金兽");
				put("他", "千古X君");
			}
		};

		AtomicInteger ai = new AtomicInteger();
		decs.entrySet().parallelStream().forEach(e -> {
			fixedThreadPool.execute(new Pipeline<>(this::fixName, e.getKey())
					.addPipelineHook(new PipelineHook((cxt, ex, args) -> { System.err.println(StringUtilx.fmt("{} [{}]", ex.getMessage(), getCxtInfo(args)));ex.printStackTrace(); cxt.setPreventThrow();/**/}))
					.add(this::buildGoods, e.getValue(), ai)
					.add(this::decorate, (new Date()).toString(), System.getenv("HOME"), System.currentTimeMillis(), Math.PI, (System.currentTimeMillis() % 2 == 0))
					.add(s -> { System.err.println(s); }));
		});

		TimeUnit.SECONDS.sleep(3l);

		fixedThreadPool.shutdown();
		fixedThreadPool.awaitTermination(10000, TimeUnit.MILLISECONDS);

	}
	
	private String decorate(String source, String a, String b, long c, double d, boolean e) {
//		if(e) {
//			double x = 1/0;
//			System.err.println(x);
//		}
		return StringUtilx.fmt("OK! a: {}, b: {}, c: {}, d:{}, source: {}", a, b, c, d, source);
	}
	
	private final static String getCxtInfo(Object[] argCxt) {
		if(null == argCxt || 0 == argCxt.length)
			return "!! [no args]";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<argCxt.length; i++) {
			if(null == argCxt[i])
				break;
			sb.append(", arg");
			sb.append(i);
			sb.append(": ");
			sb.append(argCxt[i].toString());
		}
		return "!! " + (sb.toString().substring(2));
	}
}
