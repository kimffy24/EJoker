package pro.jk.ejoker.common.system.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.jk.ejoker.common.system.constant.ConfigKeyPrimary;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IFunction1;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public final class ControlTable {

    private final static Logger logger = LoggerFactory.getLogger(ControlTable.class);

    public static void provideRegisterTuple(RegisterTuple... rts) {
        if(null == rts || rts.length==0)
            return;
        for(RegisterTuple rt : rts) {
            REGISTER_TUPLE_DICT.put(rt.key, rt);
        }
    }

    public static void initOnce() {
        if(!CONFIG_CACHE_STORE.isEmpty())
            return;
        REGISTER_TUPLE_DICT.forEach((_k, rt) -> ControlTable.getConfigValue(rt));
    }

    public static String getConfigValue(String key, String defaultOverride) {
        // 在第一个配置值提供出去之前，都是可以通过provide方法来写入配置项的。
        initOnce();

        RegisterTuple registerTuple = REGISTER_TUPLE_DICT.get(key);
        if(null == registerTuple) {
            return defaultOverride;
        }
        return getConfigValue(registerTuple);
    }

    public static String getConfigValue(String key) {
        // 在第一个配置值提供出去之前，都是可以通过provide方法来写入配置项的。
        initOnce();

        RegisterTuple registerTuple = REGISTER_TUPLE_DICT.get(key);
        if(null == registerTuple) {
            throw new RuntimeException("No key defined ! key: " + key);
        }
        return getConfigValue(registerTuple);
    }

    public static Double detectAsDouble(String key) {
        return Double.parseDouble(getConfigValue(key));
    }

    public static Integer detectAsInteger(String key) {
        return Integer.parseInt(getConfigValue(key));
    }

    public static Boolean detectAsBoolean(String key) {
        String v = getConfigValue(key);
        return "1".equals(v) || Boolean.parseBoolean(v) || "t".equalsIgnoreCase(v);
    }

    private static final Map<String, RegisterTuple> REGISTER_TUPLE_DICT = new ConcurrentHashMap<>();

    private static final Map<RegisterTuple, String> CONFIG_CACHE_STORE= new ConcurrentHashMap<>();

    // 不要直接访问这个对象，要使用 #getProperties 方法
    private static Properties properties = new Properties();

    // 让properties文件只加载1次
    private static AtomicBoolean propertiesLoad = new AtomicBoolean(false);

    synchronized private static Properties getProperties() {
        if(propertiesLoad.compareAndSet(false, true)) {
            try (FileInputStream file = new FileInputStream("e-application.properties")) {
                properties.load(file);
            } catch (IOException e) {
                // 处理文件加载错误
                logger.warn("Exception occur while load e-application.properties!", e);
            }
        }
        return properties;
    }

    /**
     * 穿透获取，从命令参数 -&gt; 环境变量 -&gt; 配置文件&lt;e-application.properties&gt;
     * @param key
     * @param keyEnvName
     * @return
     */
    private static String getConfigValueThought(String key, String keyEnvName) {

        // 从命令行参数获取配置值
        String configValue = System.getProperty(key);

        if (configValue != null && !"".equals(configValue)) {
            logger.info("\t ConfigValue[{}]={} ; <provided by System properties>",
                    key, configValue);
        } else if(null != keyEnvName && !"".equals(keyEnvName)) {
            // 如果命令行参数中没有配置值，则从环境变量获取
            configValue = System.getenv(keyEnvName);
            logger.info("\t ConfigValue[{}]={} ; <provided by ENV['{}']>",
                    key, configValue, keyEnvName);
        }

        // 如果上面两步都没得到配置值，则从 properties 文件获取
        if (configValue == null || "".equals(configValue)) {
            Properties properties = getProperties();
            configValue = properties.getProperty(key);
            logger.info("\t ConfigValue[{}]={} ; <provided by properties file e-application.properties>",
                    key, configValue);
        }

        // 如果没有配置值，则使用默认值
        if (configValue == null) {
            configValue = "";
        }

        return configValue;
    }

    private static String getConfigValue(RegisterTuple rt) {
        return CONFIG_CACHE_STORE.computeIfAbsent(rt, nRregisterTuple -> {
            String configValueThought = getConfigValueThought(nRregisterTuple.key, nRregisterTuple.envKey);
            if(null == configValueThought || "" == configValueThought) {
                // 穿透获取没有得到结果，则从默认值去取
                logger.info("\t ConfigValue[{}]={} ; <Use as default value>",
                        nRregisterTuple.key, nRregisterTuple.defaultValue);
                return nRregisterTuple.defaultValue;
            } else {
                // 得到用取值，就校验
                Boolean validateResult = nRregisterTuple.va.trigger(configValueThought);
                if(!validateResult) {
                    String errorInfo = StringUtilx.fmt(
                            "\t\t ConfigValue[{}]={} ; is validate fail. [RegisterTuple: {}]",
                            nRregisterTuple.key,
                            configValueThought,
                            rt
                    );
                    logger.warn(errorInfo);
                    throw new RuntimeException(errorInfo);
                }
                return configValueThought;
            }
        });

    }

    public static class RegisterTuple {
        public final String key;
        public final String envKey;
        public final String defaultValue;
        private final String desc;
        private final IFunction1<Boolean, String> va;

        private IFunction1<String, RegisterTuple> toStringAction = null;

        private RegisterTuple(String key, String envKey, String defaultValue, String desc, IFunction1<Boolean, String> va) {
            this.key = key;
            this.envKey = envKey;
            this.defaultValue = defaultValue;
            this.desc = desc;
            this.va = va;
        }

        @Override
        public String toString() {
            if(null != toStringAction)
                return toStringAction.trigger(this);
            return StringUtilx.fmt(
                    "[key: {}, envKey: {}, desc: {}]",
                    key,
                    envKey,
                    desc
            );
        }

        public static final RegisterTuple of(String key, String envKey, String defaultValue, String desc) {
            return new RegisterTuple(key, envKey, defaultValue, desc, v -> true);
        }

        public static final RegisterTuple of(String key, String envKey, String defaultValue, String desc, String regex) {
            Pattern p= Pattern.compile(regex);
            RegisterTuple registerTuple = new RegisterTuple(key, envKey, defaultValue, desc, v -> p.matcher(v).matches());
            registerTuple.toStringAction = rt -> StringUtilx.fmt(
                        "[key: {}, envKey: {}, desc: {}, validRegex: {}]",
                        rt.key,
                        rt.envKey,
                        rt.desc,
                        regex);
            return registerTuple;
        }

        public static final RegisterTuple of(String key, String envKey, String defaultValue, String desc, IFunction1<Boolean, String> va) {
            RegisterTuple registerTuple = new RegisterTuple(key, envKey, defaultValue, desc, va);
            registerTuple.toStringAction = rt -> StringUtilx.fmt(
                    "[key: {}, envKey: {}, desc: {}, validWithUserAction: true]",
                    rt.key,
                    rt.envKey,
                    rt.desc);
            return registerTuple;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegisterTuple)) return false;
            RegisterTuple that = (RegisterTuple) o;
            return Objects.equals(key, that.key) && Objects.equals(envKey, that.envKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, envKey);
        }
    }

    static {
        // CT同时负责基础的几个变量的而管理
        ControlTable.provideRegisterTuple(
                RegisterTuple.of(
                        ConfigKeyPrimary.EJOKER_FMT_CLEAN_INTERVAL,
                        "EJOKER_FMT_CLEAN_INTERVAL",
                        "256",
                        "EJoker fmt 方法的目标清理阈值，（单位：频次）",
                        "^\\d+$"
                ),
                RegisterTuple.of(
                        ConfigKeyPrimary.EJOKER_ASYNC_EXECUTOR_POOL_SIZE,
                        "EJOKER_ASYNC_EXECUTOR_POOL_SIZE",
                        "" + (Runtime.getRuntime().availableProcessors() * 2 + 1),
                        "EJ内部的核心异步任务池子的大小; 默认 取线程数的两倍 + 1 作为线程池大小",
                        "^\\d+$"
                ),
                RegisterTuple.of(
                        ConfigKeyPrimary.EJOKER_ASYNC_EXECUTOR_NAME_PREFIX,
                        "EJOKER_ASYNC_EXECUTOR_NAME_PREFIX",
                        "ej-worker-",
                        "EJ内部的核心异步线程的线程名",
                        "^[a-z][a-z0-9-]+-$"
                )
        );
    }

}
