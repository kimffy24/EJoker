/**
 * 搞来干嘛？虾扯蛋！
 */
/**
 * 这个包是因为使用quasar是一些jdk类对fiber不友好，而euqsar提供了自己包装的类，
 * 为了在项目中使用兼容两者 做的一些switch一样的小部件。
 * 为啥不用动态代理？？ 
 * 对动态代理没什么好感，所以不用。
 * @author JiefzzLon
 *
 */
package pro.jiefzz.ejoker.common.system.wrapper;

/**
 * 注意了哦！！！！！！！！！
 * 在这个包下的所有并发同步工具都会丢失了可中断的能力！！！
 * 自己mark一下
 *
 */