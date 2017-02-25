package com.jiefzz.ejoker.z.common.schedule;

import com.jiefzz.ejoker.z.common.action.Action;

public interface IScheduleService {

    void StartTask(String name, Action<?> action, int dueTime, int period);
    void StopTask(String name);
	
}
