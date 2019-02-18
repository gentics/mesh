package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.metric.SyncMetric;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import dagger.Lazy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_WORKER_ADDRESS;

/**
 * Verticle which will execute the elasticsearch sync.
 */
public class SyncHandler implements EventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncHandler.class);

	private Lazy<IndexHandlerRegistry> registry;

	private SearchProvider provider;

	/**
	 * Send the index sync event which will trigger the index sync job.
	 */
	public static void invokeSync() {
		Mesh.mesh().getVertx().eventBus().send(INDEX_SYNC_WORKER_ADDRESS.address, null);
	}

	@Inject
	public SyncHandler(Lazy<IndexHandlerRegistry> registry, SearchProvider provider) {
		this.registry = registry;
		this.provider = provider;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.concatArray(
			purgeOldIndices(),
			syncIndices()
		).doOnSubscribe(ignore -> {
			log.info("Processing index sync job.");
			SyncMetric.reset();
		});
		// TODO Publish event on sync finish
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(MeshEvent.INDEX_SYNC_WORKER_ADDRESS);
	}

	private Flowable<SearchRequest> syncIndices() {
		return Flowable.fromIterable(registry.get().getHandlers())
			.flatMapCompletable(handler -> handler.init().andThen(handler.syncIndices()));
	}

	private Flowable<SearchRequest> purgeOldIndices() {
		List<IndexHandler<?>> handlers = registry.get().getHandlers();
		Single<Set<String>> allIndices = provider.listIndices();
		Flowable<IndexHandler<?>> obs = Flowable.fromIterable(handlers);

		return allIndices.flatMapPublisher(indices -> obs.flatMap(handler -> {
			Set<String> unknownIndices = handler.filterUnknownIndices(indices);
			return Flowable.fromIterable(unknownIndices)
				.map(index -> client -> provider.deleteIndex(index)
				.doOnSubscribe(ignore -> log.info("Deleting unknown index {" + index + "}")));
		}));
	}
}
