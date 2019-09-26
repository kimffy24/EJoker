package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

/**
 * commandHandler处理过程中，使用的上下文就是这个上下文。<br>
 * 他能新增一个聚合根，取出聚合跟，修改聚合并提交发布，都在次上下文中提供调用
 * @author kimffy
 *
 */
public interface ICommandExecuteContext extends ICommandContext, ITrackingContext{

	public SystemFutureWrapper<Void> onCommandExecutedAsync(CommandResult commandResult);

}
