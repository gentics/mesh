package com.gentics.mesh.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotificationListener;
import rx.plugins.RxJavaPlugins;

public class RxDebugger {
	private AtomicLong counter;
	private Map<Long, DebugNotification> runningObservables;

	public void start() {
		counter = new AtomicLong();
		runningObservables = new ConcurrentHashMap<>();
		RxJavaPlugins.getInstance().registerObservableExecutionHook(new DebugHook<>(new DebugNotificationListener<Long>() {
			@Override
			public <T> Long start(DebugNotification<T> n) {
				long id = counter.getAndIncrement();
				runningObservables.put(id, n);
				return id;
			}

			@Override
			public void complete(Long id) {
				runningObservables.remove(id);
			}
		}));
	}

	public void stop() {
		//RxJavaPlugins.getInstance().reset();
	}

	public Collection<DebugNotification> getRunningObservables() {
		return runningObservables.values();
	}
}