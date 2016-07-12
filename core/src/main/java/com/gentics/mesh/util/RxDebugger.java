package com.gentics.mesh.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observer;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotification.Kind;
import rx.plugins.DebugNotificationListener;
import rx.plugins.RxJavaPlugins;

public class RxDebugger extends Thread implements Runnable {

	private Map<String, RxEventInfo> runningObservables = new ConcurrentHashMap<>();
	private long maxObsTimeInMs = 5000;
	private long checkInterval = 5000;

	private static final Logger log = LoggerFactory.getLogger(RxDebugger.class);

	@Override
	public void run() {

		RxJavaPlugins.getInstance().registerObservableExecutionHook(new DebugHook<>(new DebugNotificationListener<String>() {
			@Override
			public <T> String start(DebugNotification<T> n) {
				Observer<?> obs = n.getObserver();
				if (obs != null && (n.getKind() == Kind.OnNext || n.getKind() == Kind.Subscribe)) {
					String eventId = obs.toString() + "." + n.getKind().name();
					if (log.isDebugEnabled()) {
						log.debug("Got event: " + eventId);
					}
					try {
						throw new Exception();
					} catch (Exception e) {
						RxEventInfo eventInfo = new RxEventInfo(System.currentTimeMillis(), n, e);
						log.debug("Adding: " + eventId);
						runningObservables.put(obs.toString(), eventInfo);
						return eventId;
					}
				}
				if (obs != null && n.getKind() == Kind.OnCompleted) {
					return obs.toString() + "." + n.getKind().name();
				}
				return null;
			}

			@Override
			public void complete(String id) {
				if (id != null && id.endsWith(Kind.OnCompleted.name())) {
					String key = id.substring(0, id.lastIndexOf("."));
					log.debug("Removing: " + key);
					runningObservables.remove(key);
				}
			}

		}));

		while (true) {
			try {
				log.error("Checking for uncompleted observables..");
				checkForBlockedObs();
				Thread.sleep(checkInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void checkForBlockedObs() {
		long now = System.currentTimeMillis();
		for (Entry<String, RxEventInfo> entry : getRunningObservables().entrySet()) {
			if (log.isDebugEnabled()) {
				log.debug("Checking obserable " + entry.getKey());
			}
			long delta = now - entry.getValue().getTime();
			if (delta > maxObsTimeInMs) {
				log.error("Found observable which send last event {" + delta + "} ms ago.", entry.getValue().getThrowable());
			}
		}
	}

	public Map<String, RxEventInfo> getRunningObservables() {
		return runningObservables;
	}
}

class RxEventInfo {

	private Throwable throwable;
	private long time;
	private DebugNotification<?> info;

	public RxEventInfo(long time, DebugNotification<?> info, Throwable throwable) {
		this.time = time;
		this.info = info;
		this.throwable = throwable;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public long getTime() {
		return time;
	}

	public DebugNotification<?> getInfo() {
		return info;
	}

}
