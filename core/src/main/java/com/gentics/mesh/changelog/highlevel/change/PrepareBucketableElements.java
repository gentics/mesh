package com.gentics.mesh.changelog.highlevel.change;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.BucketableElement;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;

import dagger.Lazy;
import io.reactivex.Observable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PrepareBucketableElements extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(PrepareBucketableElements.class);

	private final Lazy<Database> db;

	private final Lazy<Vertx> vertx;

	private final Lazy<SearchProvider> searchProvider;

	private final Lazy<IndexHandlerRegistry> registry;

	private MeshOptions options;

	@Inject
	public PrepareBucketableElements(MeshOptions options, Lazy<Database> db, Lazy<Vertx> vertx, Lazy<SearchProvider> searchProvider,
		Lazy<IndexHandlerRegistry> registry) {
		this.options = options;
		this.db = db;
		this.vertx = vertx;
		this.searchProvider = searchProvider;
		this.registry = registry;
	}

	@Override
	public String getName() {
		return "PrepareBucketableElements";
	}

	@Override
	public String getUuid() {
		return "038F1F8BFF534956AF4CC056DE7B2165";
	}

	@Override
	public String getDescription() {
		return "Sets the bucketIds for all bucketable elements.";
	}

	@Override
	public void apply() {
		log.info("Applying change: " + getName());
		migrate(UserImpl.class);
		migrate(GroupImpl.class);
		migrate(RoleImpl.class);
		migrate(TagImpl.class);
		migrate(TagFamilyImpl.class);
		migrate(ProjectImpl.class);
		migrate(SchemaContainerImpl.class);
		migrate(MicroschemaContainerImpl.class);
		migrate(NodeGraphFieldContainerImpl.class);

		if (options.getSearchOptions().getUrl() != null) {
			// Now reindex the elements since the bucketId
			// needs to be in the elasticsearch documents.
			searchProvider.get().clear()
				.andThen(Observable.fromIterable(registry.get().getHandlers())
					.flatMapCompletable(handler -> handler.init()))
				.blockingAwait();

			vertx.get().setTimer(6000, id -> {
				SyncEventHandler.invokeSync(vertx.get());
			});
		}

	}

	private <T extends BucketableElement> void migrate(Class<T> clazz) {
		Iterator<? extends T> it = db.get().getVerticesForType(clazz);
		long count = 0;
		while (it.hasNext()) {
			T element = it.next();
			element.generateBucketId();
			if (count % 10_000 == 0) {
				log.info("Migrated {" + count + "} of type {" + clazz.getSimpleName() + "}");
				Tx.get().commit();
			}
			count++;
		}
	}
}
