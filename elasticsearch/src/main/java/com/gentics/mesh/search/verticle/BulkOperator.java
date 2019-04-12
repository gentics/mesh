package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An operator for Observables that bulks elastic search requests together.
 * This will emit all non-bulkable requests immediately.
 * Bulkable requests will be held back and bundled together. The bundled request will be emitted when one of the
 * following happens:
 * <ul>
 *     <li>The bulk timer has reached its limit</li>
 *     <li>The request amount limit is reached</li>
 *     <li>The flush method is called</li>
 *     <li>The upstream has emitted a complete notification</li>
 * </ul>
 */
public class BulkOperator implements ObservableOperator<SearchRequest, SearchRequest> {
	private static final Logger log = LoggerFactory.getLogger(BulkOperator.class);

	private final Vertx vertx;
	private final long bulkTime;
	private final int requestLimit;
	private FlushSubscriber<SearchRequest> subscriber;

	public BulkOperator(Vertx vertx, Duration bulkTime, int requestLimit) {
		this.vertx = vertx;
		this.bulkTime = bulkTime.toMillis();
		this.requestLimit = requestLimit;
	}

	@Override
	public Observer<? super SearchRequest> apply(Observer<? super SearchRequest> observer) {
		if (subscriber != null) {
			log.warn("More than one observer for the same operator detected. Flush will only work for the newest observer.");
		}
		subscriber = new FlushSubscriber<SearchRequest>() {
			private Long timer;
			Disposable sub;
			Queue<Bulkable> bulkableRequests = new ConcurrentLinkedQueue<>();

			@Override
			public void onSubscribe(Disposable s) {
				sub = s;
				observer.onSubscribe(s);
			}

			@Override
			public void onNext(SearchRequest elasticSearchRequest) {
				if (sub.isDisposed()) {
					cleanup();
					return;
				}
				if (elasticSearchRequest instanceof Bulkable) {
					if (bulkableRequests.isEmpty()) {
						resetTimer();
					}
					bulkableRequests.add((Bulkable) elasticSearchRequest);
					if (bulkableRequests.size() >= requestLimit) {
						log.trace("Flushing {} requests because size limit of {} has been reached.",
							bulkableRequests.size(), requestLimit);
						flush();
					}
				} else {
					log.trace("Flushing {} requests because non-bulkable request of class {{}} has been received.",
						bulkableRequests.size(), elasticSearchRequest.getClass().getSimpleName());
					flush();
					observer.onNext(elasticSearchRequest);
				}
			}

			private void resetTimer() {
				if (bulkTime > 0) {
					cancelTimer();
					timer = vertx.setTimer(bulkTime, l -> {
						log.trace("Flushing {} requests because time limit of {}ms has been reached.",
							bulkableRequests.size(), bulkTime);
						flush();
					});
				}
			}

			private void cancelTimer() {
				if (timer != null) {
					vertx.cancelTimer(timer);
					timer = null;
				}
			}

			@Override
			public void flush() {
				cancelTimer();
				if (!bulkableRequests.isEmpty() && !sub.isDisposed()) {
					BulkRequest request = new BulkRequest(new ArrayList<>(bulkableRequests));
					bulkableRequests.clear();
					observer.onNext(request);
				}
			}

			@Override
			public boolean bulking() {
				return timer != null;
			}

			@Override
			public void onError(Throwable t) {
				if (sub.isDisposed()) {
					cleanup();
					return;
				}
				observer.onError(t);
			}

			@Override
			public void onComplete() {
				if (sub.isDisposed()) {
					cleanup();
					return;
				}
				flush();
				observer.onComplete();
			}

			private void cleanup() {
				cancelTimer();
			}
		};
		return subscriber;
	}

	/**
	 * Bundles the bulkable requests and flushes a single {@link BulkRequest} if there is at least one bulkable request.
	 */
	public void flush() {
		log.info("Manually flushing bulked requests");
		if (subscriber != null) {
			subscriber.flush();
		}
	}

	/**
	 * Tests if there are requests that are currently held back and waiting to be bulked.
	 * @return
	 */
	public boolean bulking() {
		if (subscriber != null) {
			return subscriber.bulking();
		} else {
			return false;
		}
	}

	interface FlushSubscriber<T> extends Observer<T> {
		void flush();
		boolean bulking();
	}
}
