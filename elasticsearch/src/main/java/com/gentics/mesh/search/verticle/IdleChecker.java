package com.gentics.mesh.search.verticle;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gentics.mesh.search.verticle.eventhandler.Util.dummyObject;


public class IdleChecker {
	private static final Logger log = LoggerFactory.getLogger(IdleChecker.class);

	private final AtomicInteger requests = new AtomicInteger();
	private final AtomicInteger transformations = new AtomicInteger();
	private final Subject<Object> idling = PublishSubject.create();

	private final ElasticSearchOptions options;

	@Inject
	public IdleChecker(MeshOptions options) {
		this.options = options.getSearchOptions();
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
		return requests.get() == 0 && transformations.get() == 0;
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
		return transformations.incrementAndGet();
	}

	/**
	 * Subtracts a pending transformation.
	 * @return
	 */
	public int decrementAndGetTransformations() {
		return checkIdle(transformations.decrementAndGet());
	}

	/**
	 * Adds an amount of requests
	 * @param i
	 * @return
	 */
	public int addAndGetRequests(int i) {
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
	}
}
