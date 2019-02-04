package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.search.impl.SearchClient;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;

public class ElasticsearchProcessVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessVerticle.class);

	private final Eventhandler eventhandler;
	private final SearchClient elasticSearchClient;
	private final BulkOperator bulker;

	private Subject<MessageEvent> requests = PublishSubject.create();

	@Inject
	public ElasticsearchProcessVerticle(Eventhandler eventhandler, SearchClient elasticSearchClient) {
		this.eventhandler = eventhandler;
		this.elasticSearchClient = elasticSearchClient;
		bulker = new BulkOperator(vertx);
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
		bulker.flush();
	}

	private void assemble() {
		requests.toFlowable(BackpressureStrategy.MISSING)
			.onBackpressureBuffer(1000)
			.concatMap(this::generateRequests)
			.lift(bulker)
			.concatMap(request -> request.execute(elasticSearchClient).toFlowable()
				.doOnSubscribe(ignore -> log.trace("Sending request to ElasticSearch: " + request)))
			.subscribe();
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
