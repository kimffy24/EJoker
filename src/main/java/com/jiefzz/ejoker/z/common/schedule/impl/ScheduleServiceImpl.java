package com.jiefzz.ejoker.z.common.schedule.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;

@EService
public class ScheduleServiceImpl implements IScheduleService {

	private Map<String, Timer> timerTaskList = new ConcurrentHashMap<String, Timer>();
	
	public ScheduleServiceImpl() {
		Scavenger.addFianllyJob(new Runnable() {
			@Override
			public void run() {
				Set<Entry<String,Timer>> entrySet = ScheduleServiceImpl.this.timerTaskList.entrySet();
				for(Entry<String,Timer> entry:entrySet) {
					Timer timer = entry.getValue();
					try {
						timer.cancel();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				ScheduleServiceImpl.this.timerTaskList.clear();
			}
		});
	}
	
	@Override
	public void StartTask(String name, Runnable action, long dueTime, long period) {
		Timer timer;
		timerTaskList.put(name, (timer = new Timer()));
        timer.schedule(new RemindTask(action), dueTime, period);
	}

	@Override
	public void StopTask(String name) {
		Timer timer;
		if(null != (timer = timerTaskList.getOrDefault(name, null))) {
			timer.cancel();
		}
	}

    public static class RemindTask extends TimerTask {
    	
    	private final Runnable action;
    	
    	public RemindTask(Runnable action) {
    		this.action = action;
    	}
    	
        public void run() {
        	action.run();
        }
    }
    
	
}
