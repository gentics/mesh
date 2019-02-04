package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.search.impl.SearchClient;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticsearchProcessVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessVerticle.class);

	private final Eventhandler eventhandler;
	private final SearchClient elasticSearchClient;
	private BulkOperator bulker;

	private Subject<MessageEvent> requests = PublishSubject.create();
	private AtomicInteger pendingRequests = new AtomicInteger();

	@Inject
	public ElasticsearchProcessVerticle(Eventhandler eventhandler, SearchClient elasticSearchClient) {
		this.eventhandler = eventhandler;
		this.elasticSearchClient = elasticSearchClient;
	}

	@Override
	public void start() {
		assemble();

		eventhandler.getHandledEvents()
			.forEach(event -> vertx.eventBus().<String>consumer(event.address, message ->
				requests.onNext(new MessageEvent(event, MeshEventModel.fromMessage(message)))
			));
	}

	@Override
	public void stop() {
		requests.onComplete();
	}

	/**
	 * Flushes the buffer of elastic search requests and dispatches all pending requests.
	 */
	public void flush() {
		if (bulker != null) {
			bulker.flush();
		}
	}

	/**
	 * Refreshes the ElasticSearch indices so that all changes are readable
	 * @return
	 */
	public Completable refresh() {
		return elasticSearchClient.refresh().async()
			.doOnSuccess(response -> {
				if (log.isTraceEnabled()) {
					log.trace("Refreshing response from ElasticSearch:\n" + response.encodePrettily());
				}
			}).toCompletable()
			.doOnSubscribe(ignore -> log.trace("Refreshing all ElasticSearch indices..."));
	}

	/**
	 * Tests if the verticle has any queued requests.
	 * @return
	 */
	public boolean isIdle() {
		return pendingRequests.get() == 0 && !bulker.bulking();
	}

	private void assemble() {
		bulker = new BulkOperator(vertx);
		requests.toFlowable(BackpressureStrategy.MISSING)
			.onBackpressureBuffer(1000)
			.concatMap(this::generateRequests)
			.lift(bulker)
			.doOnNext(ignore -> pendingRequests.incrementAndGet())
			.concatMap(request -> request.execute(elasticSearchClient).andThen(Flowable.just(request))
				.doOnSubscribe(ignore -> log.trace("Sending request to ElasticSearch: " + request)))
			.subscribe(request -> {
				log.trace("Request completed: " + request);
				pendingRequests.decrementAndGet();
				if (isIdle()) {
					log.trace("All requests completed. Sending idle event");
					vertx.eventBus().send(MeshEvent.SEARCH_IDLE.address, null);
				}
			});
	}

	private Flowable<ElasticSearchRequest> generateRequests(MessageEvent messageEvent) {
		try {
			return Flowable.fromIterable(this.eventhandler.handle(messageEvent));
		} catch (Exception e) {
			// TODO Error handling
			e.printStackTrace();
			return Flowable.empty();
		}
	}
}
