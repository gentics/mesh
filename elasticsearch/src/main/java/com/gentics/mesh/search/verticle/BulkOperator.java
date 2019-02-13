package com.gentics.mesh.search.verticle;

import com.gentics.mesh.search.verticle.request.BulkRequest;
import com.gentics.mesh.search.verticle.request.Bulkable;
import com.gentics.mesh.search.verticle.request.ElasticsearchRequest;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.Duration;
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
public class BulkOperator implements ObservableOperator<ElasticsearchRequest, ElasticsearchRequest> {
	private static final Logger log = LoggerFactory.getLogger(BulkOperator.class);

	private final Vertx vertx;
	private final long bulkTime;
	private final int requestLimit;
	private FlushSubscriber<ElasticsearchRequest> subscriber;

	public BulkOperator(Vertx vertx) {
		this(vertx, Duration.ofSeconds(10), 1000);
	}

	public BulkOperator(Vertx vertx, Duration bulkTime, int requestLimit) {
		this.vertx = vertx;
		this.bulkTime = bulkTime.toMillis();
		this.requestLimit = requestLimit;
	}

	@Override
	public Observer<? super ElasticsearchRequest> apply(Observer<? super ElasticsearchRequest> observer) {
		log.warn("More than one observer for the same operator detected. Flush will only work for the newest observer.");
		subscriber = new FlushSubscriber<ElasticsearchRequest>() {
			private Long timer;
			Disposable sub;
			Queue<Bulkable> bulkableRequests = new ConcurrentLinkedQueue<>();

			@Override
			public void onSubscribe(Disposable s) {
				sub = s;
				observer.onSubscribe(s);
			}

			@Override
			public void onNext(ElasticsearchRequest elasticSearchRequest) {
				if (elasticSearchRequest instanceof Bulkable) {
					if (bulkableRequests.isEmpty()) {
						resetTimer();
					}
					bulkableRequests.add((Bulkable) elasticSearchRequest);
					if (bulkableRequests.size() >= requestLimit) {
						flush();
					}
				} else {
					flush();
					observer.onNext(elasticSearchRequest);
				}
			}

			private void resetTimer() {
				if (bulkTime > 0) {
					cancelTimer();
					timer = vertx.setTimer(bulkTime, l -> flush());
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
				if (!bulkableRequests.isEmpty()) {
					BulkRequest request = new BulkRequest(bulkableRequests);
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
				observer.onError(t);
			}

			@Override
			public void onComplete() {
				flush();
				observer.onComplete();
			}
		};
		return subscriber;
	}

	/**
	 * Bundles the bulkable requests and flushes a single {@link BulkRequest} if there is at least one bulkable request.
	 */
	public void flush() {
		log.info("Flushing bulked requests");
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
