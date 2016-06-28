package com.gentics.mesh.graphdb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;

public class TransactionContextScheduler extends Scheduler {

	private static final Handler<AsyncResult<Object>> NOOP = result -> {
	};

	private final Vertx vertx;
	private final boolean blocking;
	private final boolean ordered;
	private final RxJavaSchedulersHook schedulersHook = RxJavaPlugins.getInstance().getSchedulersHook();;
	private final Context context;
	private final Database database;

	public TransactionContextScheduler(Context context, boolean blocking, Database database) {
		this(context, blocking, true, database);
	}

	public TransactionContextScheduler(Context context, boolean blocking, boolean ordered, Database database) {
		this.vertx = context.owner();
		this.context = context;
		this.blocking = blocking;
		this.ordered = ordered;
		this.database = database;
	}

	public TransactionContextScheduler(Vertx vertx, boolean blocking, Database database) {
		this(vertx, blocking, true, database);
	}

	public TransactionContextScheduler(Vertx vertx, boolean blocking, boolean ordered, Database database) {
		this.vertx = vertx;
		this.context = null;
		this.blocking = blocking;
		this.ordered = ordered;
		this.database = database;
	}

	@Override
	public Worker createWorker() {
		return new WorkerImpl();
	}

	private static final Object DUMB = new JsonObject();

	private class WorkerImpl extends Worker {

		private final ConcurrentHashMap<TimedAction, Object> actions = new ConcurrentHashMap<>();
		private final AtomicBoolean cancelled = new AtomicBoolean();

		@Override
		public Subscription schedule(Action0 action) {
			return schedule(action, 0, TimeUnit.MILLISECONDS);
		}

		@Override
		public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
			action = schedulersHook.onSchedule(action);
			TimedAction timed = new TimedAction(action, unit.toMillis(delayTime), 0);
			actions.put(timed, DUMB);
			return timed;
		}

		@Override
		public Subscription schedulePeriodically(Action0 action, long initialDelay, long period, TimeUnit unit) {
			action = schedulersHook.onSchedule(action);
			TimedAction timed = new TimedAction(action, unit.toMillis(initialDelay), unit.toMillis(period));
			actions.put(timed, DUMB);
			return timed;
		}

		@Override
		public void unsubscribe() {
			if (cancelled.compareAndSet(false, true)) {
				actions.keySet().forEach(TimedAction::unsubscribe);
			}
		}

		@Override
		public boolean isUnsubscribed() {
			return cancelled.get();
		}

		class TimedAction implements Subscription, Runnable {

			private final Context context;
			private long id;
			private final Action0 action;
			private final long periodMillis;
			private boolean cancelled;

			public TimedAction(Action0 action, long delayMillis, long periodMillis) {
				this.context = TransactionContextScheduler.this.context != null ? TransactionContextScheduler.this.context
						: vertx.getOrCreateContext();
				this.cancelled = false;
				this.action = action;
				this.periodMillis = periodMillis;
				if (delayMillis > 0) {
					schedule(delayMillis);
				} else {
					id = -1;
					execute(null);
				}
			}

			private void schedule(long delay) {
				this.id = vertx.setTimer(delay, this::execute);
			}

			private void execute(Object o) {
				if (blocking) {
					context.executeBlocking(this::run, ordered, NOOP);
				} else {
					context.runOnContext(this::run);
				}
			}

			private void run(Object arg) {
				run();
			}

			@Override
			public void run() {

				try (NoTrx noTx = database.noTrx()) {
					synchronized (TimedAction.this) {
						if (cancelled) {
							return;
						}
					}
					action.call();
					synchronized (TimedAction.this) {
						if (periodMillis > 0) {
							schedule(periodMillis);
						}
					}
				}
			}

			@Override
			public synchronized void unsubscribe() {
				if (!cancelled) {
					actions.remove(this);
					if (id > 0) {
						vertx.cancelTimer(id);
					}
					cancelled = true;
				}
			}

			@Override
			public synchronized boolean isUnsubscribed() {
				return cancelled;
			}
		}
	}
}
