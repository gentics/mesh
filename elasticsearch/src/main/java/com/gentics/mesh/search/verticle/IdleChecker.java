package com.gentics.mesh.search.verticle;

import static com.gentics.mesh.search.verticle.eventhandler.Util.dummyObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.metric.SearchRequestMetric;
import com.gentics.mesh.metric.MetricsService;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class IdleChecker {
	private static final Logger log = LoggerFactory.getLogger(IdleChecker.class);

	private final AtomicInteger requests = new AtomicInteger();
	private final AtomicInteger transformations = new AtomicInteger();
	private final Subject<Object> idling = PublishSubject.create();

	private final ElasticSearchOptions options;

	private AtomicInteger idleGauge;

	@Inject
	public IdleChecker(MeshOptions options, MetricsService metrics) {
		this.options = options.getSearchOptions();
		if (metrics != null && metrics.isEnabled()) {
			metrics.getMetricRegistry().gauge(SearchRequestMetric.REQUESTS.key(), requests);
			metrics.getMetricRegistry().gauge(SearchRequestMetric.TRANSFORMATIONS.key(), transformations);
			idleGauge = metrics.getMetricRegistry().gauge(SearchRequestMetric.IDLE.key(), new AtomicInteger());
			// test for idle to set the idleGauge to the correct value
			isIdle();
		}
	}

	/**
	 * Returns a hot observable that emits a dummy object whenever the search is idle.
	 * @return
	 */
	public Observable<Object> idling() {
		return idling.debounce(options.getIdleDebounceTime(), TimeUnit.MILLISECONDS)
			.filter(ignore -> isIdle());
	}

	/**
	 * Completes the {@link #idling()} observable.
	 */
	public void close() {
		idling.onComplete();
	}

	/**
	 * Tests if search has currently no pending transformations or requests.
	 * @return
	 */
	public boolean isIdle() {
		boolean idle = requests.get() == 0 && transformations.get() == 0;
		if (this.idleGauge != null) {
			this.idleGauge.set(idle ? 1 : 0);
		}
		return idle;
	}

	/**
	 * The amount of pending requests.
	 * @return
	 */
	public int getRequests() {
		return requests.get();
	}

	/**
	 * The amount of pending event transformations.
	 * @return
	 */
	public int getTransformations() {
		return transformations.get();
	}

	/**
	 * Adds a pending transformation.
	 * @return
	 */
	public int incrementAndGetTransformations() {
		int value = transformations.incrementAndGet();
		isIdle();
		return value;
	}

	/**
	 * Subtracts a pending transformation.
	 * @return
	 */
	public int decrementAndGetTransformations() {
		// When dropping all events it can happen that pending transformations are not dropped. This is to prevent
		// the counter to go below 0.
		return checkIdle(transformations.updateAndGet(x -> x <= 0
			? 0
			: x - 1
		));
	}

	/**
	 * Adds an amount of requests
	 * @param i
	 * @return
	 */
	public int addAndGetRequests(int i) {
		log.trace("Adding {} requests", i);
		return checkIdle(requests.addAndGet(i));
	}

	private int checkIdle(int value) {
		log.trace("Idle check invoked. Remaining requests: {}, remaining transformations: {}",
			requests.get(), transformations.get());
		if (isIdle()) {
			idling.onNext(dummyObject);
		}
		return value;
	}

	/**
	 * Resets the amount of pending transformations.
	 */
	public void resetTransformations() {
		transformations.set(0);
		isIdle();
	}
}
