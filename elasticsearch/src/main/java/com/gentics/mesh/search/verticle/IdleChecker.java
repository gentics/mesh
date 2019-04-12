package com.gentics.mesh.search.verticle;

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

	@Inject
	public IdleChecker() {

	}

	public Observable<Object> idling() {
		return idling.debounce(100, TimeUnit.MILLISECONDS)
			.filter(ignore -> isIdle());
	}

	public void close() {
		idling.onComplete();
	}

	private boolean isIdle() {
		return requests.get() == 0 && transformations.get() == 0;
	}

	public int getRequests() {
		return requests.get();
	}

	public int getTransformations() {
		return transformations.get();
	}

	public int incrementAndGetRequests() {
		return requests.incrementAndGet();
	}

	public int incrementAndGetTransformations() {
		return transformations.incrementAndGet();
	}

	public int decrementAndGetRequests() {
		return checkIdle(requests.decrementAndGet());
	}

	public int decrementAndGetTransformations() {
		return checkIdle(transformations.decrementAndGet());
	}

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


	public void resetTransformations() {
		transformations.set(0);
	}
}
