package com.gentics.mesh.search.verticle;

import io.reactivex.FlowableOperator;
import io.vertx.core.Vertx;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BulkOperator implements FlowableOperator<ElasticSearchRequest, ElasticSearchRequest> {
	private final Vertx vertx;
	private final long bulkTime;
	private final int requestLimit;

	public BulkOperator(Vertx vertx) {
		this(vertx, Duration.ofSeconds(10), 1000);
	}

	public BulkOperator(Vertx vertx, Duration bulkTime, int requestLimit) {
		this.vertx = vertx;
		this.bulkTime = bulkTime.toMillis();
		this.requestLimit = requestLimit;
	}

	@Override
	public Subscriber<? super ElasticSearchRequest> apply(Subscriber<? super ElasticSearchRequest> observer) throws Exception {
		return new Subscriber<ElasticSearchRequest>() {
			private Long timer;
			Subscription sub;
			List<Bulkable> bulkableRequests = new ArrayList<>(requestLimit);

			@Override
			public void onSubscribe(Subscription s) {
				sub = s;
				observer.onSubscribe(s);
			}

			@Override
			public void onNext(ElasticSearchRequest elasticSearchRequest) {
				if (elasticSearchRequest instanceof Bulkable) {
					if (bulkableRequests.isEmpty()) {
						resetTimer();
					}
					bulkableRequests.add((Bulkable) elasticSearchRequest);
					if (bulkableRequests.size() >= requestLimit) {
						flushBulk();
					}
				} else {
					flushBulk();
					observer.onNext(elasticSearchRequest);
				}
			}

			private void resetTimer() {
				cancelTimer();
				timer = vertx.setTimer(bulkTime, l -> flushBulk());
			}

			private void cancelTimer() {
				if (timer != null) {
					vertx.cancelTimer(timer);
					timer = null;
				}
			}

			private void flushBulk() {
				cancelTimer();
				if (!bulkableRequests.isEmpty()) {
					BulkRequest request = new BulkRequest(bulkableRequests);
					bulkableRequests.clear();
					observer.onNext(request);
				}
			}

			@Override
			public void onError(Throwable t) {
				observer.onError(t);
			}

			@Override
			public void onComplete() {
				flushBulk();
				observer.onComplete();
			}
		};
	}
}
