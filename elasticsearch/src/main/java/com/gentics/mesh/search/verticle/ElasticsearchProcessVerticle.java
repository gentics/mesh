package com.gentics.mesh.search.verticle;

import com.gentics.mesh.event.MeshEventModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;

import javax.inject.Inject;
import java.util.stream.Stream;

import static com.gentics.mesh.Events.EVENT_USER_CREATED;
import static com.gentics.mesh.Events.EVENT_USER_DELETED;
import static com.gentics.mesh.Events.EVENT_USER_UPDATED;

public class ElasticsearchProcessVerticle extends AbstractVerticle {

	private Subject<MessageEvent> requests = PublishSubject.create();
	private final Eventhandler eventhandler;

	@Inject
	public ElasticsearchProcessVerticle(Eventhandler eventhandler) {
		this.eventhandler = eventhandler;
	}

	@Override
	public void start() {
		assemble();

		Stream.of(EVENT_USER_CREATED, EVENT_USER_UPDATED, EVENT_USER_DELETED)
			.forEach(event -> vertx.eventBus().<MeshEventModel>consumer(event, message -> requests.onNext(new MessageEvent(event, message.body()))));
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
			.concatMap(request -> request.execute().toFlowable())
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
