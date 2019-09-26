package pro.jiefzz.ejoker.commanding;

public interface ICommandRoutingKeyProvider {

	public String getRoutingKey(ICommand command);
	
}
