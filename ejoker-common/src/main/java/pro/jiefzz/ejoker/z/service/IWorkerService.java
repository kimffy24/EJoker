package pro.jiefzz.ejoker.z.service;

public interface IWorkerService {

	public IWorkerService start();
	
    public IWorkerService shutdown();
    
    default public IWorkerService stop() {
    	return this.shutdown();
    }
    
}
