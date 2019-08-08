package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_START;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.request.DropIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.metric.SyncMetric;
import com.gentics.mesh.search.verticle.MessageEvent;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Verticle which will execute the elasticsearch sync.
 */
public class SyncEventHandler implements EventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);

	private final Lazy<IndexHandlerRegistry> registry;

	private final SearchProvider provider;

	private final Vertx vertx;

	/**
	 * Send the index sync event which will trigger the index sync job.
	 */
	public static void invokeSync(Vertx vertx) {
		log.info("Sending sync event");
		vertx.eventBus().publish(INDEX_SYNC_REQUEST.address, null);
	}

	public static Completable invokeSyncCompletable(Mesh mesh) {
		return MeshEvent.doAndWaitForEvent(mesh, INDEX_SYNC_FINISHED, () -> SyncEventHandler.invokeSync(mesh.getVertx()));
	}

	public static void invokeClear(Vertx vertx) {
		vertx.eventBus().publish(INDEX_CLEAR_REQUEST.address, null);
	}

	public static Completable invokeClearCompletable(Mesh mesh) {
		return MeshEvent.doAndWaitForEvent(mesh, INDEX_CLEAR_FINISHED, () -> SyncEventHandler.invokeClear(mesh.getVertx()));
	}

	@Inject
	public SyncEventHandler(Lazy<IndexHandlerRegistry> registry, SearchProvider provider, Vertx vertx) {
		this.registry = registry;
		this.provider = provider;
		this.vertx = vertx;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return generateSyncRequests();
	}

	public Flowable<SearchRequest> generateSyncRequests() {
		return Flowable.concatArray(
			purgeOldIndices(),
			syncIndices(),
			publishSyncEndEvent()
		).doOnSubscribe(ignore -> {
			log.info("Processing index sync job.");
			vertx.eventBus().publish(INDEX_SYNC_START.address, null);
			SyncMetric.reset();
		});
	}

	/**
	 * This is actually not a search event, but instead just signals the end of an index sync.
	 * TODO Find a better way to do this, this will not work on an error
	 */
	private Flowable<SearchRequest> publishSyncEndEvent() {
		return Flowable.just(SearchRequest.create(provider -> {
			log.debug("Sending sync complete event");
			vertx.eventBus().publish(INDEX_SYNC_FINISHED.address, null);
			return Completable.complete();
		}));
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(INDEX_SYNC_REQUEST);
	}

	private Flowable<SearchRequest> syncIndices() {
		return Flowable.fromIterable(registry.get().getHandlers())
			.flatMap(handler ->
				handler.init()
					.doOnSubscribe(ignore -> log.debug("Init for {}", handler.getClass()))
					.doOnComplete(() -> log.debug("Init for {} complete", handler.getClass()))
				.andThen(handler.syncIndices()
					.doOnSubscribe(ignore -> log.debug("Syncing for {}", handler.getClass()))
			));
	}

	private Flowable<SearchRequest> purgeOldIndices() {
		List<IndexHandler<?>> handlers = registry.get().getHandlers();
		Single<Set<String>> allIndices = provider.listIndices();
		Flowable<IndexHandler<?>> obs = Flowable.fromIterable(handlers);

		return allIndices.flatMapPublisher(indices -> obs.flatMap(handler -> {
			Set<String> unknownIndices = handler.filterUnknownIndices(indices);
			return Flowable.fromIterable(unknownIndices)
				.map(DropIndexRequest::new);
		}));
	}
}
