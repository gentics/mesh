package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.impl.SearchClient;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;

import javax.inject.Inject;

public class ElasticsearchProcessVerticle extends AbstractVerticle {

	private final Eventhandler eventhandler;
	private final SearchClient elasticSearchClient;

	private Subject<MessageEvent> requests = PublishSubject.create();

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

	private void assemble() {
		requests.toFlowable(BackpressureStrategy.MISSING)
			.onBackpressureBuffer(1000)
			.concatMap(this::generateRequests)
			.lift(new BulkOperator(vertx))
			.concatMap(request -> request.execute(elasticSearchClient).toFlowable())
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
