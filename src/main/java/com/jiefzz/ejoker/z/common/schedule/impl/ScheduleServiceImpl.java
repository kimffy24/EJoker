package com.jiefzz.ejoker.z.common.schedule.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;

@EService
public class ScheduleServiceImpl implements IScheduleService {

	private Map<String, Timer> timerTaskList = new HashMap<>();

	@Dependence
	private Scavenger scavenger;

	@EInitialize
	private void init() {
		scavenger.addFianllyJob(this::cleanAll);
	}

	@Override
	public void startTask(String name, Runnable action, long dueTime, long period) {
		Timer timer;
		if (null != timerTaskList.putIfAbsent(name, (timer = new Timer())))
			throw new RuntimeException(String.format("Task name of [%s] is exists before!!!", name));
		timer.schedule(new RemindTask(action), dueTime, period);
	}

	@Override
	public void stopTask(String name) {
		Timer timer;
		if (null != (timer = timerTaskList.getOrDefault(name, null))) {
			timer.cancel();
		}
	}

	private void cleanAll() {
		Set<Entry<String, Timer>> entrySet = timerTaskList.entrySet();
		for (Entry<String, Timer> entry : entrySet) {
			Timer timer = entry.getValue();
			timer.cancel();
		}
		timerTaskList.clear();
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
