package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CHECK_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CHECK_START;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CHECK_FINISHED;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.verticle.MessageEvent;

import dagger.Lazy;
import io.reactivex.Flowable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Event handler, that will check the currently existing indices (for existence and correctness of the mapping)
 */
@Singleton
public class CheckIndicesHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(CheckIndicesHandler.class);

	private final Lazy<IndexHandlerRegistry> registry;

	private final Vertx vertx;

	/**
	 * Create an instance
	 * @param registry index handler registry
	 * @param vertx vertx
	 */
	@Inject
	public CheckIndicesHandler(Lazy<IndexHandlerRegistry> registry, Vertx vertx) {
		this.registry = registry;
		this.vertx = vertx;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(INDEX_CHECK_REQUEST);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return syncIndices().doOnSubscribe(ignore -> {
			log.debug("Processing index check job.");
			vertx.eventBus().publish(INDEX_CHECK_START.address, null);
		}).doFinally(() -> {
			log.debug("Index check job finished.");
			vertx.eventBus().publish(INDEX_CHECK_FINISHED.address, null);
		});
	}

	protected Flowable<SearchRequest> syncIndices() {
		return Flowable.fromIterable(registry.get().getHandlers())
				.flatMap(handler -> handler.check().toFlowable());
	}
}
