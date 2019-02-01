package com.gentics.mesh.search.verticle;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.mesh.event.MeshEventModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;

public class ElasticsearchProcessVerticle extends AbstractVerticle {

	private final Eventhandler eventhandler;
	private final ElasticsearchClient<JsonObject> elasticSearchClient;

	private Subject<MessageEvent> requests = PublishSubject.create();

	@Inject
	public ElasticsearchProcessVerticle(Eventhandler eventhandler, ElasticsearchClient<JsonObject> elasticSearchClient) {
		this.eventhandler = eventhandler;
		this.elasticSearchClient = elasticSearchClient;
	}

	@Override
	public void start() {
		assemble();

		eventhandler.getHandledEvents()
			.forEach(event -> vertx.eventBus().<MeshEventModel>consumer(event.address, message ->
				requests.onNext(new MessageEvent(event, message.body()))
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
