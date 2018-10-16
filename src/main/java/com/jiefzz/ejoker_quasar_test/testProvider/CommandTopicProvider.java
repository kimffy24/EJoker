package com.jiefzz.ejoker_quasar_test.testProvider;

import java.util.Set;

import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class CommandTopicProvider implements ITopicProvider<ICommand> {

	@Override
	public String getTopic(ICommand source) {
		return "TopicEJokerCommand";
	}

	@Override
	public Set<String> GetAllTopics() {
		return null;
	}

}
