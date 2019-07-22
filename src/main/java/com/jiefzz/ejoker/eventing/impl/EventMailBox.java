package com.jiefzz.ejoker.eventing.impl;

import java.util.List;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.eventing.EventCommittingContext;
import com.jiefzz.ejoker.infrastructure.AbstractAggregateMessageMailBox;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

public class EventMailBox extends AbstractAggregateMessageMailBox<EventCommittingContext, Void> {

	public EventMailBox(String aggregateRootId, IVoidFunction1<List<EventCommittingContext>> handleMessageAction,
			SystemAsyncHelper systemAsyncHelper) {
		super(aggregateRootId, EJokerEnvironment.MAX_BATCH_EVENTS, true, null,
				x -> {
					handleMessageAction.trigger(x);
					return SystemFutureWrapperUtil.completeFuture();
				},
				systemAsyncHelper);
	}

}
