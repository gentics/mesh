package com.gentics.mesh.changelog;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;

import java.io.File;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.cli.AbstractBootstrapInitializer;
import com.gentics.mesh.cli.CoreVerticleLoader;
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.cli.PostProcessFlags;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.core.data.dao.ChangelogDao;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootResolver;
import com.gentics.mesh.core.data.service.ServerSchemaStorageImpl;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.distributed.DistributedEventManager;
import com.gentics.mesh.distributed.coordinator.MasterElector;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinationTopology;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.event.EventBusStore;
import com.gentics.mesh.hibernate.data.dao.RoleDaoImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.monitor.liveness.EventBusLivenessManager;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.IndexHandlerRegistryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;

import dagger.Lazy;
import io.reactivex.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.metrics.MetricsOptions;

/**
 * A bootstrap initializer for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibernateBootstrapInitializerImpl extends AbstractBootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(HibernateBootstrapInitializerImpl.class);

	private final HibernateDatabase db;

	private final HibPermissionRoots permRoots;

	private final RootResolver rootResolver;

	private final ChangelogDao changelogDao;

	private final ContentCachedStorage contentCachedStorage;

	private final Runnable SYNC_INDEX_ACTION = (() -> {
		if (searchProvider instanceof TrackingSearchProviderImpl) {
			return;
		}
		if (searchProvider instanceof DevNullSearchProvider) {
			return;
		}
		if (searchProvider.getClient() == null) {
			return;
		}
		// Ensure indices are setup and sync the documents
		log.info("Invoking index sync. This may take some time.");
		SyncEventHandler.invokeSyncCompletable(mesh()).blockingAwait();
		log.info("Index sync completed.");
	});

	@Inject
	public HibernateBootstrapInitializerImpl(ServerSchemaStorageImpl schemaStorage, HibernateDatabase db,
			SearchProvider searchProvider, BCryptPasswordEncoder encoder, DistributedEventManager eventManager,
			Lazy<IndexHandlerRegistryImpl> indexHandlerRegistry, Lazy<CoreVerticleLoader> loader,
			HighLevelChangelogSystem highlevelChangelogSystem, CacheRegistry cacheRegistry,
			MeshPluginManager pluginManager, MeshOptions options, RouterStorageRegistryImpl routerStorageRegistry,
			MetricsOptions metricsOptions, LocalConfigApi localConfigApi, BCryptPasswordEncoder passwordEncoder,
			MasterElector coordinatorMasterElector, LivenessManager liveness, EventBusLivenessManager eventbusLiveness,
			EventBusStore eventBusStore, ContentCachedStorage contentCachedStorage, HibPermissionRoots permRoots, RootResolver rootResolver, ChangelogDao changelogDao) {
		super(schemaStorage, db, searchProvider, passwordEncoder, eventManager, indexHandlerRegistry, loader,
				highlevelChangelogSystem, cacheRegistry, pluginManager, options, routerStorageRegistry, metricsOptions,
				localConfigApi, passwordEncoder, coordinatorMasterElector, liveness, eventbusLiveness, eventBusStore);
		this.contentCachedStorage = contentCachedStorage;
		this.db = db;
		this.rootResolver = rootResolver;
		this.permRoots = permRoots;
		this.changelogDao = changelogDao;
	}

	@Override
	public void init(Mesh mesh, boolean forceResync, MeshOptions options, MeshCustomLoader<Vertx> verticleLoader)
			throws Exception {
		// this is necessary, so that hibernate will log using slf4j
		System.setProperty("org.jboss.logging.provider", "slf4j");
		super.init(mesh, forceResync, options, verticleLoader);

		// if the interval for periodic check for stale transactions is set (greater than 0), we schedule the check
		if (options instanceof HibernateMeshOptions) {
			long checkInterval = ((HibernateMeshOptions)options).getStorageOptions().getStaleTxCheckInterval();
			if (checkInterval > 0) {
				vertx.setPeriodic(checkInterval, id -> {
					// check for the curren thread (which is expected to be an eventloop thread)
					cleanupLeftoverTx();
					// also execute blocking, which will check in one of the worker threads
					vertx.executeBlocking(prom -> {
						cleanupLeftoverTx();
					}, false);
				});
			}
		}

	}

	/**
	 * Dispose of any transaction, which is stored as threadlocal for the current thread
	 */
	private void cleanupLeftoverTx() {
		String threadName = Thread.currentThread().getName();
		log.debug("Check for leftover tx in {}", threadName);
		Tx.maybeGet().ifPresent(tx -> {
			// found a transaction, set it to be failed, since we want to roll it back
			tx.failure();
			try {
				// close the transaction now
				log.warn("Closing leftover tx in {}", threadName);
				tx.close();
			} catch (Throwable ignored) {
			}
		});
	}

	@Override
	public RootResolver rootResolver() {
		return rootResolver;
	}

	@Override
	public void markChangelogApplied() {
		log.info("Initial setup. Marking all found changelog entries as applied");
		highlevelChangelogSystem.markAllAsApplied(changelogDao);
		log.info("All changes marked");
	}

	@Override
	protected void initPermissionRoots(Tx tx) {
		loadPermissionRoots();
	}

	@Override
	public void globalCacheClear() {
		super.globalCacheClear();
		contentCachedStorage.evictAll();
	}

	private void loadPermissionRoots() {
		permRoots.project();
		permRoots.user();
		permRoots.group();
		permRoots.role();
		permRoots.microschema();
		permRoots.schema();
		permRoots.mesh();
	}

	@Override
	public void initOptionalData(Tx tx, boolean isEmptyInstallation) {
	}

	@Override
	public void initDatabaseTypes() {
		// DB types are automatically initted ar Hibernate start, if required.
	}

	@Override
	public void initPermissions() {
		db.tx(tx -> {
			HibernateTx htx = tx.unwrap();
			RoleDaoImpl roleDao = htx.roleDao();
			HibRole adminRole = roleDao.findByName("admin");

			Arrays.asList(
					htx.loadAll(htx.projectDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.nodeDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.tagDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.schemaDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.microschemaDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.userDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.groupDao().getPersistenceClass()).map(HibBaseElement.class::cast),
					htx.loadAll(htx.roleDao().getPersistenceClass()).map(HibBaseElement.class::cast)
				).stream()
			.flatMap(Function.identity())
			.forEach(element -> {
					roleDao.grantPermissions(adminRole, element, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM, PUBLISH_PERM, READ_PUBLISHED_PERM);
					if (log.isTraceEnabled()) {
						log.trace("Granting admin CRUD permissions on vertex {" + element.getUuid() + "} for role {" + adminRole.getUuid() + "}");
					}
				});
			tx.success();
		});
	}

	@Override
	public void invokeChangelog(PostProcessFlags flags) {
		log.info("Invoking database changelog check...");
		// Run the high level changelog entries
		highlevelChangelogSystem.apply(flags, changelogDao, null);
		log.info("Changelog completed.");
	}

	@Override
	public void invokeChangelogInCluster(PostProcessFlags flags, MeshOptions configuration) {
		log.info("Invoking database changelog check...");
		if (requiresChangelog(filter -> !filter.isAllowedInCluster(configuration))) {
			// Joining of cluster members is only allowed when the changelog has been applied
			throw new RuntimeException(
					"The instance can't join the cluster since the cluster database does not contain all needed changes. Please restart a single instance in the cluster with the "
							+ MeshOptions.MESH_CLUSTER_INIT_ENV + " environment flag or the -" + MeshCLI.INIT_CLUSTER + " command line argument to migrate the database.");
		}
		doWithLock(GLOBAL_CHANGELOG_LOCK_KEY, "executing changelog", executeChangelog(flags, configuration), 60 * 1000).subscribe();
	}

	/**
	 * Run the processing of cluster-allowed highlevel changelog items.
	 * 
	 * @param flags
	 * @param configuration
	 * @return
	 */
	protected Completable executeChangelog(PostProcessFlags flags, MeshOptions configuration) {
		return Completable.defer(() -> {
			// Now run the high level changelog entries, which are allowed to be executed in cluster mode
			highlevelChangelogSystem.apply(flags, changelogDao, filter -> filter.isAllowedInCluster(configuration));

			log.info("Changelog completed.");
			return Completable.complete();
		});
	}

	@Override
	public void syncIndex() {
		SYNC_INDEX_ACTION.run();
	}

	@Override
	public boolean requiresChangelog(Predicate<? super HighLevelChange> filter) {
		return highlevelChangelogSystem.requiresChanges(changelogDao, filter);
	}

	@Override
	protected Database db() {
		return db;
	}

	@Override
	protected void initStandalone(MeshOptions options, PostProcessFlags flags, boolean isInitMode) throws Exception {
		// TODO it is technically possible to embed HSQLDB.
	}

	@Override
	protected void initCluster(MeshOptions options, PostProcessFlags flags, boolean isInitMode) throws Exception {
		ClusterOptions clusterOptions = options.getClusterOptions();
		if (clusterOptions.getNetworkHost() == null) {
			String localIp = getLocalIpForRoutedRemoteIP("8.8.8.8");
			log.info("No networkHost setting was specified within the cluster settings. Using the determined IP {" + localIp + "}.");
			clusterOptions.setNetworkHost(localIp);
		}

		// This setting will be referenced by the hazelcast configuration
		System.setProperty("mesh.clusterName", clusterOptions.getClusterName());

		final String defaultHazelcastConfigPath = new File("").getAbsolutePath() + File.separator + "config" + File.separator + "hazelcast.xml";
		final String hazelcastConfig = System.getProperty("hazelcast.config", defaultHazelcastConfigPath);
		// This setting is used by the HazelcastClusterManager to identify the path of the hazelcast configuration file
		System.setProperty("vertx.hazelcast.config", hazelcastConfig);

		// request delegation based on cluster membership is a specific feature of an own cluster manager implementation, which we currently don't have
		if (clusterOptions.getCoordinatorMode() != CoordinatorMode.DISABLED) {
			log.warn("Coordination mode " + clusterOptions.getCoordinatorMode() + " will be ignored. Setting coordination mode to DISABLED");
		}
		clusterOptions.setCoordinatorMode(CoordinatorMode.DISABLED);

		// coordination topology management is a specific feature of an own cluster manager implementation, which we currently don't have
		if (clusterOptions.getCoordinatorTopology() != CoordinationTopology.UNMANAGED) {
			log.warn("Coordination topology " + clusterOptions.getCoordinatorTopology() + " will be ignored. Setting coordination topology to UNMANAGED");
		}
		clusterOptions.setCoordinatorTopology(CoordinationTopology.UNMANAGED);

		initVertx(options);

		initLocalData(flags, options, !isInitMode);

		boolean active = false;
		while (!active) {
			log.info("Waiting for hazelcast to become active");
			active = db().clusterManager().getHazelcast().getLifecycleService().isRunning();
			if (active) {
				break;
			}
			Thread.sleep(1000);
		}
		coordinatorMasterElector.start();
	}

	@Override
	public int getClusteredVertxInitializationTimeoutInSeconds() {
		// hibernate might be doing the initial schema setup, which requires quite some time
		return 30;
	}
}
