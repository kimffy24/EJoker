package pro.jiefzz.ejoker.common.system.extension.pipeline;

import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.functional.IFunction1;
import pro.jiefzz.ejoker.common.system.functional.IFunction2;
import pro.jiefzz.ejoker.common.system.functional.IFunction3;
import pro.jiefzz.ejoker.common.system.functional.IFunction4;
import pro.jiefzz.ejoker.common.system.functional.IFunction5;
import pro.jiefzz.ejoker.common.system.functional.IFunction6;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction2;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction3;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction4;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction5;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction6;

public class Pipeline<R> implements Runnable {

	private final IFunction<R> task;

	private MiddlePipeline<R, ?> next = null;

	private EndPipeline<R> end = null;
	
	private PipelineHook hook = null;
	
	private Object[] argCxt = null;
	
	public Pipeline(IFunction<R> t) {
		this.task = t;
	}

	public <T1> Pipeline(IFunction1<R, T1> t, T1 arg1) {
		this.task = () -> t.trigger(arg1);
		argCxt = new Object[] { arg1 };
	}

	public <T1, T2> Pipeline(IFunction2<R, T1, T2> t, T1 arg1, T2 arg2) {
		this.task = () -> t.trigger(arg1, arg2);
		argCxt = new Object[] { arg1, arg2 };
	}

	public <T1, T2, T3> Pipeline(IFunction3<R, T1, T2, T3> t, T1 arg1, T2 arg2, T3 arg3) {
		this.task = () -> t.trigger(arg1, arg2, arg3);
		argCxt = new Object[] { arg1, arg2, arg3 };
	}

	public <T1, T2, T3, T4> Pipeline(IFunction4<R, T1, T2, T3, T4> t, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		this.task = () -> t.trigger(arg1, arg2, arg3, arg4);
		argCxt = new Object[] { arg1, arg2, arg3, arg4 };
	}

	public <T1, T2, T3, T4, T5> Pipeline(IFunction5<R, T1, T2, T3, T4, T5> t, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
		this.task = () -> t.trigger(arg1, arg2, arg3, arg4, arg5);
		argCxt = new Object[] { arg1, arg2, arg3, arg4, arg5 };
	}

	public <T1, T2, T3, T4, T5, T6> Pipeline(IFunction6<R, T1, T2, T3, T4, T5, T6> t, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) {
		this.task = () -> t.trigger(arg1, arg2, arg3, arg4, arg5, arg6);
		argCxt = new Object[] { arg1, arg2, arg3, arg4, arg5, arg6 };
	}

	public <RT> MiddlePipeline<R, RT> add(IFunction1<RT, R> nextTask) {
		if (null == next && null == end) {
			MiddlePipeline<R, RT> n = new MiddlePipeline<>(nextTask);
			this.next = n;
			return n;
		}
		throw new RuntimeException("Unsupport multi pipeline!!!");
	}

	public <RT, T1> MiddlePipeline<R, RT> add(IFunction2<RT, R, T1> nextTaskx, T1 arg1) {
		return add(r -> { return nextTaskx.trigger(r, arg1); });
	}

	public <RT, T1, T2> MiddlePipeline<R, RT> add(IFunction3<RT, R, T1, T2> nextTaskx, T1 arg1, T2 arg2) {
		return add(r -> { return nextTaskx.trigger(r, arg1, arg2); });
	}

	public <RT, T1, T2, T3> MiddlePipeline<R, RT> add(IFunction4<RT, R, T1, T2, T3> nextTaskx, T1 arg1, T2 arg2, T3 arg3) {
		return add(r -> { return nextTaskx.trigger(r, arg1, arg2, arg3); });
	}

	public <RT, T1, T2, T3, T4> MiddlePipeline<R, RT> add(IFunction5<RT, R, T1, T2, T3, T4> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		return add(r -> { return nextTaskx.trigger(r, arg1, arg2, arg3, arg4); });
	}

	public <RT, T1, T2, T3, T4, T5> MiddlePipeline<R, RT> add(IFunction6<RT, R, T1, T2, T3, T4, T5> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
		return add(r -> { return nextTaskx.trigger(r, arg1, arg2, arg3, arg4, arg5); });
	}

	public Runnable add(IVoidFunction1<R> nextTask) {
		if (null == next && null == end) {
			EndPipeline<R> e = new EndPipeline<>(nextTask);
			this.end = e;
			return e;
		}
		throw new RuntimeException("Unsupport multi pipeline!!!");
	}
	
	public <T1> Runnable add(IVoidFunction2<R, T1> nextTaskx, T1 arg1) {
		return add(r -> { nextTaskx.trigger(r, arg1); });
	}
	
	public <T1, T2> Runnable add(IVoidFunction3<R, T1, T2> nextTaskx, T1 arg1, T2 arg2) {
		return add(r -> { nextTaskx.trigger(r, arg1, arg2); });
	}
	
	public <T1, T2, T3> Runnable add(IVoidFunction4<R, T1, T2, T3> nextTaskx, T1 arg1, T2 arg2, T3 arg3) {
		return add(r -> { nextTaskx.trigger(r, arg1, arg2, arg3); });
	}

	public <T1, T2, T3, T4> Runnable add(IVoidFunction5<R, T1, T2, T3, T4> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
		return add(r -> { nextTaskx.trigger(r, arg1, arg2, arg3, arg4); });
	}

	public <T1, T2, T3, T4, T5> Runnable add(IVoidFunction6<R, T1, T2, T3, T4, T5> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
		return add(r -> { nextTaskx.trigger(r, arg1, arg2, arg3, arg4, arg5); });
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run() {
		MiddlePipeline n = next;
		MiddlePipeline pre = null;
		try {
			Object r = task.trigger();
			if(null != hook && null != hook.aspecter) {
				hook.aspecter.trigger(r);
			}
			while (null != n) {
				r = n.task.trigger(r);
				if(null != hook && null != hook.aspecter) {
					hook.aspecter.trigger(r);
				}
				pre = n;
				n = n.next;
			}
			// all pipeline's add method permitted that only one is exists in property `next` and `end`.
			if(null != pre) {
				EndPipeline e = pre.end;
				if(null != e)
					e.task.trigger(r);
			} else {
				if(null != this.end)
					this.end.task.trigger((R )r);
			}
		} catch (Exception e) {
			if(null != this.hook && null != this.hook.exceptionHandler) {
				this.hook.exceptionHandler.trigger(this.hook, e, null == pre ? this.argCxt : pre.argCxt);
				if(this.hook.isPreventThrow())
					return;
			}
			throw new RuntimeException(StringUtilx.fmt("Pipeline exec faild!!! [{}]", null == pre ? getCxtInfo(this.argCxt) : getCxtInfo(pre.argCxt)), e);
		}
	}
	
	public Pipeline<R> addPipelineHook(PipelineHook hook) {
		this.hook = hook;
		return this;
	}
	
	private final static String getCxtInfo(Object[] argCxt) {
		if(null == argCxt || 0 == argCxt.length)
			return "[no args]";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<argCxt.length; i++) {
			if(null == argCxt[i])
				break;
			sb.append(", arg");
			sb.append(i);
			sb.append(": ");
			sb.append(argCxt[i].toString());
		}
		return sb.toString().substring(2);
	}
	
	public abstract class AbstractPipeline implements Runnable {

		@Override
		public void run() {
			Pipeline.this.run();
		}

	}

	/**
	 * 
	 * @author kimffy
	 *
	 * @param <C> 此管道消费的类型
	 * @param <P> 此管道产出的类型
	 */
	public final class MiddlePipeline<C, P> extends AbstractPipeline {

		private final IFunction1<P, C> task;

		private MiddlePipeline<P, ?> next = null;

		private EndPipeline<P> end = null;
		
		private Object[] argCxt = new Object[] { null, null, null, null, null, null};

		private MiddlePipeline(IFunction1<P, C> task) {
			this.task = task;
		}

		public <RT> MiddlePipeline<P, RT> add(IFunction1<RT, P> nextTaskx) {
			if (null == next && null == end) {
				MiddlePipeline<P, RT> n = new MiddlePipeline<>(r -> { argCxt[0] = r; return nextTaskx.trigger(r); });
				this.next = n;
				return n;
			}
			throw new RuntimeException("Unsupport multi pipeline!!!");
		}

		public <RT, T1> MiddlePipeline<P, RT> add(IFunction2<RT, P, T1> nextTaskx, T1 arg1) {
			return add(r -> {
				argCxt[1] = arg1;
				return nextTaskx.trigger(r, arg1);
			});
		}

		public <RT, T1, T2> MiddlePipeline<P, RT> add(IFunction3<RT, P, T1, T2> nextTaskx, T1 arg1, T2 arg2) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				return nextTaskx.trigger(r, arg1, arg2);
			});
		}

		public <RT, T1, T2, T3> MiddlePipeline<P, RT> add(IFunction4<RT, P, T1, T2, T3> nextTaskx, T1 arg1, T2 arg2, T3 arg3) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				argCxt[3] = arg3;
				return nextTaskx.trigger(r, arg1, arg2, arg3);
			});
		}

		public <RT, T1, T2, T3, T4> MiddlePipeline<P, RT> add(IFunction5<RT, P, T1, T2, T3, T4> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				argCxt[3] = arg3;
				argCxt[4] = arg4;
				return nextTaskx.trigger(r, arg1, arg2, arg3, arg4);
			});
		}

		public <RT, T1, T2, T3, T4, T5> MiddlePipeline<P, RT> add(IFunction6<RT, P, T1, T2, T3, T4, T5> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				argCxt[3] = arg3;
				argCxt[4] = arg4;
				argCxt[5] = arg5;
				return nextTaskx.trigger(r, arg1, arg2, arg3, arg4, arg5);
			});
		}

		public Runnable add(IVoidFunction1<P> nextTaskx) {
			if (null == next && null == end) {
				EndPipeline<P> e = new EndPipeline<>(r -> { argCxt[0] = r; nextTaskx.trigger(r); });
				this.end = e;
				return e;
			}
			throw new RuntimeException("Unsupport multi pipeline!!!");
		}
		
		public <T1> Runnable add(IVoidFunction2<P, T1> nextTaskx, T1 arg1) {
			return add(r -> {
				argCxt[1] = arg1;
				nextTaskx.trigger(r, arg1);
			});
		}
		
		public <T1, T2> Runnable add(IVoidFunction3<P, T1, T2> nextTaskx, T1 arg1, T2 arg2) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				nextTaskx.trigger(r, arg1, arg2);
			});
		}
		
		public <T1, T2, T3> Runnable add(IVoidFunction4<P, T1, T2, T3> nextTaskx, T1 arg1, T2 arg2, T3 arg3) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				argCxt[3] = arg3;
				nextTaskx.trigger(r, arg1, arg2, arg3);
			});
		}

		public <T1, T2, T3, T4> Runnable add(IVoidFunction5<P, T1, T2, T3, T4> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				argCxt[3] = arg3;
				argCxt[4] = arg4;
				nextTaskx.trigger(r, arg1, arg2, arg3, arg4);
			});
		}

		public <T1, T2, T3, T4, T5> Runnable add(IVoidFunction6<P, T1, T2, T3, T4, T5> nextTaskx, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
			return add(r -> {
				argCxt[1] = arg1;
				argCxt[2] = arg2;
				argCxt[3] = arg3;
				argCxt[4] = arg4;
				argCxt[5] = arg5;
				nextTaskx.trigger(r, arg1, arg2, arg3, arg4, arg5);
			});
		}

	}

	/**
	 * 
	 * @author kimffy
	 *
	 * @param <C> 此管道的消费类型
	 */
	public final class EndPipeline<C> extends AbstractPipeline {

		private final IVoidFunction1<C> task;

		private EndPipeline(IVoidFunction1<C> task) {
			this.task = task;
		}

	}

}
