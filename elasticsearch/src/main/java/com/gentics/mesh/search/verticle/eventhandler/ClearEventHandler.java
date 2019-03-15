package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.verticle.MessageEvent;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_REQUEST;

import java.util.Collection;
import java.util.Collections;

@Singleton
public class ClearEventHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(ClearEventHandler.class);

	private final Vertx vertx;

	@Inject
	public ClearEventHandler(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.just(SearchRequest.create(provider -> provider.clear()
			.andThen(Completable.fromAction(() -> vertx.eventBus().publish(INDEX_CLEAR_FINISHED.address, null)))
			.doOnSubscribe(ignore -> log.info("Clearing indices"))
			.doOnComplete(() -> log.info("Clearing indices complete")))
		);
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(INDEX_CLEAR_REQUEST);
	}
}
