package com.jiefzz.ejoker.z.common.schedule;

public interface IScheduleService {

    void StartTask(String name, Runnable action, long dueTime, long period);
    void StopTask(String name);
	
}
