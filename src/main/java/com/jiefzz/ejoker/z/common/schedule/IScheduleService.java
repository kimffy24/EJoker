package com.jiefzz.ejoker.z.common.schedule;

public interface IScheduleService {

    void StartTask(String name, Runnable action, int dueTime, int period);
    void StopTask(String name);
	
}
