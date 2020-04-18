package pro.jk.ejoker.common.service;

public interface IWorkerService {

	public IWorkerService start();
	
    public IWorkerService shutdown();
    
    default public IWorkerService stop() {
    	return this.shutdown();
    }
    
    public boolean isAllReady();
}
