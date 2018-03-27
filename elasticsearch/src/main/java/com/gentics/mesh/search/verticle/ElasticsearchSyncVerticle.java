package com.gentics.mesh.search.verticle;

import static com.gentics.mesh.Events.INDEX_SYNC_WORKER_ADDRESS;

import javax.inject.Inject;

import com.gentics.mesh.Events;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.metric.SyncMetric;
import com.gentics.mesh.verticle.AbstractJobVerticle;

import dagger.Lazy;
import io.reactivex.Observable;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Verticle which will execute the elasticsearch sync.
 */
public class ElasticsearchSyncVerticle extends AbstractJobVerticle {

	private static final Logger log = LoggerFactory.getLogger(ElasticsearchSyncVerticle.class);

	private static final String GLOBAL_SYNC_LOCK_NAME = "mesh.internal.synclock";

	private Lazy<IndexHandlerRegistry> registry;

	private SearchProvider provider;

	/**
	 * Send the index sync event which will trigger the index sync job.
	 */
	public static void invokeSync() {
		Mesh.mesh().getVertx().eventBus().send(INDEX_SYNC_WORKER_ADDRESS, null);
	}

	@Inject
	public ElasticsearchSyncVerticle(Lazy<IndexHandlerRegistry> registry, SearchProvider provider) {
		this.registry = registry;
		this.provider = provider;
	}

	public String getJobAdress() {
		return INDEX_SYNC_WORKER_ADDRESS;
	}

	@Override
	public String getLockName() {
		return GLOBAL_SYNC_LOCK_NAME;
	}

	public void executeJob(Message<Object> message) {
		log.info("Got index sync request.");
		SyncMetric.reset();

		Observable.fromIterable(registry.get().getHandlers())
			.flatMapCompletable(handler -> handler.init().andThen(handler.syncIndices()))
			.andThen(provider.refreshIndex()).subscribe(() -> {
				vertx.eventBus().publish(Events.INDEX_SYNC_EVENT, new JsonObject().put("status", "completed"));
				log.info("Sync completed");
			}, error -> {
				log.error("Sync failed", error);
				vertx.eventBus().publish(Events.INDEX_SYNC_EVENT, new JsonObject().put("status", "failed"));
			});
	}

}
