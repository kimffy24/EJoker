package pro.jk.ejoker.common.system.constant;

public final class ConfigKeyPrimary {

    /**
     * EJoker fmt 方法的目标清理阈值，（单位：频次）
     */
    public static final String EJOKER_FMT_CLEAN_INTERVAL = "ejoker.common.fmt.clean.interval";

    /**
     * EJ内部的核心异步任务池子的大小; 默认 取线程数的两倍 + 1 作为线程池大小
     */
    public static final String EJOKER_ASYNC_EXECUTOR_POOL_SIZE = "ejoker.common.async.pool.size";

    /**
     * EJ内部的核心异步线程的线程名
     */
    public static final String EJOKER_ASYNC_EXECUTOR_NAME_PREFIX = "ejoker.common.async.pool.thread.name.prefix";

}
