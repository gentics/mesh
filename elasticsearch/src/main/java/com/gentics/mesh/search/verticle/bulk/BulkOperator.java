package com.gentics.mesh.search.verticle.bulk;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.data.search.request.SearchRequest;

import io.reactivex.FlowableOperator;
import io.reactivex.internal.util.BackpressureHelper;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * An operator for Observables that bulks elastic search requests together.
 * This will emit all non-bulkable requests immediately.
 * Bulkable requests will be held back and bundled together.
 * The bundled request will be emitted if the subscriber requests items and when one of the
 * following happens:
 * <ul>
 *     <li>The bulk timer has reached its limit</li>
 *     <li>The request amount limit is reached</li>
 *     <li>The total request size limit is reached</li>
 *     <li>The flush method is called</li>
 *     <li>The upstream has emitted a complete notification</li>
 * </ul>
 */
public class BulkOperator implements FlowableOperator<SearchRequest, SearchRequest> {
	private static final Logger log = LoggerFactory.getLogger(BulkOperator.class);

	private final Vertx vertx;
	private final long bulkTime;
	private final int requestLimit;
	private final long lengthLimit;
	private ActualBulkOperator<SearchRequest> operator;

	public BulkOperator(Vertx vertx, Duration bulkTime, int requestLimit, long lengthLimit) {
		this.vertx = vertx;
		this.bulkTime = bulkTime.toMillis();
		this.requestLimit = requestLimit;
		this.lengthLimit = lengthLimit;
	}

	@Override
	public Subscriber<? super SearchRequest> apply(Subscriber<? super SearchRequest> subscriber) throws Exception {
		if (operator != null) {
			log.warn("More than one subscriber for the same operator detected. Flush will only work for the newest subscriber.");
		}
		operator = new ActualBulkOperator<SearchRequest>() {
			private boolean upstreamCompleted = false;
			private final AtomicLong requested = new AtomicLong(0);
			private final AtomicBoolean canceled = new AtomicBoolean(false);
			private Subscription subscription;
			private final BulkQueue bulkableRequests = new BulkQueue();
			private final AtomicReference<SearchRequest> outstandingNonBulkableRequest = new AtomicReference<>();

			private final BulkTimer timer = new BulkTimer(vertx, bulkTime, () -> {
				log.trace("Flushing {} requests because time limit of {}ms has been reached.",
					bulkableRequests.size(), bulkTime);
				flush();
			});

			@Override
			public void flush() {
				timer.stop();
				if (!canceled.get() && requested.get() > 0 && !bulkableRequests.isEmpty()) {
					log.trace("Emitting bulk of size {} to subscriber", bulkableRequests.size());
					BulkRequest request = new BulkRequest(bulkableRequests.asList());
					bulkableRequests.clear();
					if (log.isInfoEnabled()) {
						log.info("Sending bulk to elasticsearch:\n{}", request);
					}
					subscriber.onNext(request);
					BackpressureHelper.produced(requested, 1);
				}

				if (!canceled.get() && requested.get() > 0 && outstandingNonBulkableRequest.get() != null) {
					log.trace("Emitting remaining non bulkable request to subscriber", bulkableRequests.size());
					subscriber.onNext(outstandingNonBulkableRequest.getAndSet(null));
					BackpressureHelper.produced(requested, 1);
				}

				if (upstreamCompleted && bulkableRequests.isEmpty() && outstandingNonBulkableRequest.get() == null) {
					log.trace("Sending onComplete event to subscriber");
					subscriber.onComplete();
				}

				request();
			}

			private void request() {
				if (!canceled.get() && requested.get() > 0 && !upstreamCompleted) {
					log.trace("Requesting 1 item from upstream");
					subscription.request(1);
				}
			}

			@Override
			public boolean bulking() {
				return timer.isRunning();
			}

			@Override
			public void onSubscribe(Subscription s) {
				subscription = s;
				log.trace("Calling onSubscribe of subscriber");
				subscriber.onSubscribe(this);
			}

			@Override
			public void onNext(SearchRequest searchRequest) {
				log.trace("Search request of class [{}] received from upstream.",
					searchRequest.getClass().getSimpleName());

				if (canceled.get()) {
					return;
				}
				if (searchRequest instanceof Bulkable) {
					if (bulkableRequests.isEmpty()) {
						timer.restart();
					}
					bulkableRequests.add((Bulkable) searchRequest);
					log.trace("Added request of class [{}] to the current bulk with the size of now {}.",
						searchRequest.getClass().getSimpleName(), bulkableRequests.size());
					if (bulkableRequests.size() >= requestLimit || bulkableRequests.getBulkLength() >= lengthLimit) {
						if (log.isTraceEnabled()) {
							if (bulkableRequests.size() >= requestLimit) {
								log.info("Flushing {} requests because request amount limit of {} has been reached.",
									bulkableRequests.size(), requestLimit);
							} else {
								log.info("Flushing {} requests with total size of {} because size limit of {} has been exceeded.",
									bulkableRequests.size(), bulkableRequests.getBulkLength(), lengthLimit);
							}
						}
						flush();
					} else {
						request();
					}
				} else {
					log.trace("Flushing {} requests because non-bulkable request of class {{}} has been received.",
						bulkableRequests.size(), searchRequest.getClass().getSimpleName());
					outstandingNonBulkableRequest.set(searchRequest);
					flush();
				}
			}

			@Override
			public void onError(Throwable t) {
				log.trace("Error event from upstream received: {}", t);
				if (canceled.get()) {
					return;
				}
				canceled.set(true);
				subscriber.onError(t);
			}

			@Override
			public void onComplete() {
				log.trace("Completed event from upstream received");
				if (canceled.get()) {
					return;
				}
				upstreamCompleted = true;
				flush();
			}

			@Override
			public void request(long n) {
				log.trace("Downstream requested {} items", n);
				BackpressureHelper.add(requested, n);
				flush();
			}

			@Override
			public void cancel() {
				log.trace("Downstream canceled subscription");
				canceled.set(true);
				subscription.cancel();
			}
		};
		return operator;
	}

	/**
	 * Bundles the bulkable requests and flushes a single {@link BulkRequest} if there is at least one bulkable request.
	 */
	public void flush() {
		log.info("Manually flushing bulked requests");
		if (operator != null) {
			operator.flush();
		}
	}

	/**
	 * Tests if there are requests that are currently held back and waiting to be bulked.
	 * @return
	 */
	public boolean bulking() {
		if (operator != null) {
			return operator.bulking();
		} else {
			return false;
		}
	}

	interface ActualBulkOperator<T> extends Subscription, Subscriber<T> {
		void flush();
		boolean bulking();
	}
}
