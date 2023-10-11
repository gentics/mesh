package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_START;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.request.DropIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.search.SearchIndexSyncEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.IndexHandlerRegistryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.MessageEvent;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Verticle which will execute the elasticsearch sync.
 */
public class SyncEventHandler implements EventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);

	private final Lazy<IndexHandlerRegistryImpl> registry;

	private final SearchProvider provider;

	private final Vertx vertx;

	private final SyncMetersFactory syncMetersFactory;

	private final MeshOptions options;

	/**
	 * Send the index sync event which will trigger the index sync job.
	 * @param indexPattern optional index pattern
	 */
	public static void invokeSync(Vertx vertx, String indexPattern) {
		SearchIndexSyncEventModel eventModel = new SearchIndexSyncEventModel().setIndexPattern(indexPattern);
		log.info("Sending sync event for index pattern {}", eventModel.getIndexPattern());
		vertx.eventBus().publish(INDEX_SYNC_REQUEST.address, new JsonObject(JsonUtil.toJson(eventModel, true)));
	}

	/**
	 * Invoke the sync via event and wait for the finished event. Once received the completable will be completed.
	 * 
	 * @param mesh
	 * @return
	 */
	public static Completable invokeSyncCompletable(Mesh mesh) {
		return invokeSyncCompletable(mesh.getVertx());
	}

	/**
	 * Invoke the sync via event and wait for the finished event. Once received the completable will be completed.
	 * 
	 * @param vertx
	 * @return
	 */
	public static Completable invokeSyncCompletable(Vertx vertx) {
		return MeshEvent.doAndWaitForEvent(vertx, INDEX_SYNC_FINISHED, () -> SyncEventHandler.invokeSync(vertx, null));
	}

	/**
	 * Publish the index clear event which will trigger the clear process.
	 * 
	 * @param vertx
	 */
	public static void invokeClear(Vertx vertx) {
		vertx.eventBus().publish(INDEX_CLEAR_REQUEST.address, null);
	}

	/**
	 * Invoke the index clear event and complete only when the finished event was received.
	 * 
	 * @param mesh
	 * @return
	 */
	public static Completable invokeClearCompletable(Mesh mesh) {
		return MeshEvent.doAndWaitForEvent(mesh, INDEX_CLEAR_FINISHED, () -> SyncEventHandler.invokeClear(mesh.getVertx()));
	}

	/**
	 * Invoke the index clear event and complete only when the finished event was received.
	 * 
	 * @param vertx
	 * @return
	 */
	public static Completable invokeClearCompletable(Vertx vertx) {
		return MeshEvent.doAndWaitForEvent(vertx, INDEX_CLEAR_FINISHED, () -> SyncEventHandler.invokeClear(vertx));
	}

	@Inject
	public SyncEventHandler(Lazy<IndexHandlerRegistryImpl> registry, SearchProvider provider, Vertx vertx, SyncMetersFactory syncMetersFactory, MeshOptions options) {
		this.registry = registry;
		this.provider = provider;
		this.vertx = vertx;
		this.syncMetersFactory = syncMetersFactory;
		this.options = options;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		String indexPattern = ".*";
		if (messageEvent.message instanceof SearchIndexSyncEventModel) {
			indexPattern = ((SearchIndexSyncEventModel) messageEvent.message).getIndexPattern();
	}
		return generateSyncRequests(indexPattern);
	}

	/**
	 * Generate the sync requests which consist of purging the old indices, syncing the data, publishing the finished event.
	 * 
	 * @param indexPatterm
	 * @return
	 */
	public Flowable<SearchRequest> generateSyncRequests(String indexPattern) {
		return Flowable.concatArray(
			purgeOldIndices(),
			syncIndices(indexPattern),
			publishSyncEndEvent()).doOnSubscribe(ignore -> {
				log.info("Processing index sync job.");
				vertx.eventBus().publish(INDEX_SYNC_START.address, null);
				syncMetersFactory.reset();
			});
	}

	/**
	 * This is actually not a search event, but instead just signals the end of an index sync. TODO Find a better way to do this, this will not work on an error
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

	/**
	 * Return search requests needed to init and sync all indices (for all registered type handlers).
	 * 
	 * @param indexPatterm
	 * @return
	 */
	private Flowable<SearchRequest> syncIndices(String indexPattern) {
		Pattern pattern = null;
		if (indexPattern != null) {
			indexPattern = StringUtils.removeStartIgnoreCase(indexPattern, options.getSearchOptions().getPrefix());
			try {
				pattern = Pattern.compile(indexPattern);
			} catch (PatternSyntaxException e) {
				log.warn("Index pattern {} is not valid, synchronizing all indices", e, indexPattern);
			}
		}
		Optional<Pattern> optPattern = Optional.ofNullable(pattern);
		return Flowable.fromIterable(registry.get().getHandlers())
			.flatMap(handler -> handler.init()
				.doOnSubscribe(ignore -> log.debug("Init for {}", handler.getClass()))
				.doOnComplete(() -> log.debug("Init for {} complete", handler.getClass()))
				.andThen(handler.syncIndices(optPattern)
					.doOnSubscribe(ignore -> log.debug("Syncing for {}", handler.getClass()))));
	}

	/**
	 * Generate purge requests needed to drop no longer needed indices.
	 * @return
	 */
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
