package pro.jiefzz.ejoker.z.schedule;

public interface IScheduleService {

    void startTask(String name, Runnable action, long dueTime, long period);
    void stopTask(String name);
	
}
