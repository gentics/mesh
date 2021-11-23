package com.gentics.mesh.cli;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ChangelogSystemImpl;
import com.gentics.mesh.changelog.ReindexAction;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorageImpl;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.hazelcast.core.HazelcastInstance;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import io.reactivex.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

@Singleton
public class OrientDBBootstrapInitializerImpl extends AbstractBootstrapInitializer implements OrientDBBootstrapInitializer {

	private static Logger log = LoggerFactory.getLogger(OrientDBBootstrapInitializerImpl.class);

	@Inject
	public ServerSchemaStorageImpl schemaStorage;

	@Inject
	public ChangelogSystem changelogSystem;

	@Inject
	public OrientDBDatabase db;

	private MeshRoot meshRoot;

	private HazelcastClusterManager manager;

	private final ReindexAction SYNC_INDEX_ACTION = (() -> {
		// Init the classes / indices
		DatabaseHelper.init(db);

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
		log.info("Invoking index sync. This may take some time..");
		SyncEventHandler.invokeSyncCompletable(mesh()).blockingAwait();
		log.info("Index sync completed.");
	});
	
	@Inject
	public OrientDBBootstrapInitializerImpl() {
		super();
	}
	
	@Override
	public void initDatabaseTypes() {
		// Update graph indices and vertex types (This may take some time)
		DatabaseHelper.init(db);
	}
	
	@Override
	protected Vertx createClusteredVertx(MeshOptions options, VertxOptions vertxOptions) {
		HazelcastInstance hazelcast = db.clusterManager().getHazelcast();
		Objects.requireNonNull(hazelcast, "The hazelcast instance was not yet initialized.");
		manager = new HazelcastClusterManager(hazelcast);
		vertxOptions.setClusterManager(manager);
		String localIp = options.getClusterOptions().getNetworkHost();

		Integer clusterPort = options.getClusterOptions().getVertxPort();
		int vertxClusterPort = clusterPort == null ? 0 : clusterPort;

		EventBusOptions eventbus = vertxOptions.getEventBusOptions();
		eventbus.setHost(localIp);
		eventbus.setPort(vertxClusterPort);
		eventbus.setClusterPublicHost(localIp);
		eventbus.setClusterPublicPort(vertxClusterPort);

		if (log.isDebugEnabled()) {
			log.debug("Using Vert.x cluster port {" + vertxClusterPort + "}");
			log.debug("Using Vert.x cluster public port {" + vertxClusterPort + "}");
			log.debug("Binding Vert.x on host {" + localIp + "}");
		}
		CompletableFuture<Vertx> fut = new CompletableFuture<>();
		Vertx.clusteredVertx(vertxOptions, rh -> {
			log.info("Created clustered Vert.x instance");
			if (rh.failed()) {
				Throwable cause = rh.cause();
				log.error("Failed to create clustered Vert.x instance", cause);
				fut.completeExceptionally(new RuntimeException("Error while creating clusterd Vert.x instance", cause));
				return;
			}
			Vertx vertx = rh.result();
			fut.complete(vertx);
		});
		try {
			return fut.get(10, SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Error while creating clusterd Vert.x instance");
		}
	}
	@Override
	public void initMandatoryData(MeshOptions config) throws Exception {
		db.tx(tx -> {
			if (db.requiresTypeInit()) {
				MeshRoot meshRoot = meshRoot();

				// Create the initial root vertices
				meshRoot.getTagRoot();
				meshRoot.getTagFamilyRoot();
				meshRoot.getProjectRoot();
				meshRoot.getLanguageRoot();
				meshRoot.getJobRoot();
				meshRoot.getChangelogRoot();

				meshRoot.getGroupRoot();
				meshRoot.getRoleRoot();
			}
			
			if (db.requiresTypeInit()) {
				initLanguages();
			}

			schemaStorage.init();
			tx.success();
		});

	}

	@Override
	public void initOptionalData(Tx tx, boolean isEmptyInstallation) {
		if (db.requiresTypeInit()) {
			meshRoot = meshRoot();
		}
	}

	@Override
	public void initPermissions() {
		db.tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			HibRole adminRole = roleDao.findByName("admin");
			FramedTransactionalGraph graph = HibClassConverter.toGraph(tx).getGraph();
			for (Vertex vertex : graph.getVertices()) {
				WrappedVertex wrappedVertex = (WrappedVertex) vertex;
				MeshVertex meshVertex = graph.frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
				roleDao.grantPermissions(adminRole, meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM, PUBLISH_PERM, READ_PUBLISHED_PERM);
				if (log.isTraceEnabled()) {
					log.trace("Granting admin CRUD permissions on vertex {" + meshVertex.getUuid() + "} for role {" + adminRole.getUuid() + "}");
				}
			}
			tx.success();
		});
	}

	@Override
	protected Database db() {
		return db;
	}
	
	@Override
	public void clearReferences() {
		if (meshRoot != null) {
			meshRoot.clearReferences();
		}
		meshRoot = null;
		super.clearReferences();
	}

	@Override
	protected void initCluster(MeshOptions options, PostProcessFlags flags, boolean isInitMode) throws Exception {
		ClusterOptions clusterOptions = options.getClusterOptions();

		// Check whether we need to update the settings and use a determined local IP
		if (clusterOptions.getNetworkHost() == null) {
			String localIp = getLocalIpForRoutedRemoteIP("8.8.8.8");
			log.info("No networkHost setting was specified within the cluster settings. Using the determined IP {" + localIp + "}.");
			clusterOptions.setNetworkHost(localIp);
		}

		if (isInitMode) {
			log.info("Init cluster flag was found. Creating initial graph database if necessary.");
			// We need to init the graph db before starting the OrientDB Server. Otherwise the database will not get picked up by the orientdb server which
			// handles the clustering.
			db.setupConnectionPool();

			// TODO find a better way around the chicken and the egg issues.
			// Vert.x is currently needed for eventQueueBatch creation.
			// This process fails if vert.x has not been made accessible during local data setup.
			vertx = Vertx.vertx();
			initLocalData(flags, options, false);
			db.closeConnectionPool();
			db.shutdown();
			vertx.close();
			vertx = null;

			// Start OrientDB Server which will open the previously created db and init hazelcast
			db.clusterManager().startAndSync();

			// Now since hazelcast is ready we can create Vert.x
			initVertx(options);

			// Register the event handlers now since vert.x can now be used
			db.clusterManager().registerEventHandlers();

			// Setup the connection pool in order to allow transactions to be used
			db.setupConnectionPool();

			// Finally start ES integration
			searchProvider.init();
			searchProvider.start();
			pluginManager.init();
			if (flags.isReindex()) {
				createSearchIndicesAndMappings();
			}
		} else {
			// Start the server - it will block until a database could be synced.
			// We need to wait for other nodes and receive the graphdb
			db.clusterManager().startAndSync();

			// Now init vert.x since hazelcast is now ready.
			initVertx(options);

			// Register the event handlers now since vert.x can now be used
			db.clusterManager().registerEventHandlers();
			isInitialSetup = false;

			// Setup the connection pool in order to allow transactions to be used
			db.setupConnectionPool();

			// Finally start ES integration
			searchProvider.init();
			searchProvider.start();
			initLocalData(flags, options, true);
		}

		boolean active = false;
		while (!active) {
			log.info("Waiting for hazelcast to become active");
			active = manager.getHazelcastInstance().getLifecycleService().isRunning();
			if (active) {
				break;
			}
			Thread.sleep(1000);
		}
		coordinatorMasterElector.start();
	}

	public ChangelogRoot changelogRoot() {
		return meshRoot().getChangelogRoot();
	}

	/**
	 * Return the mesh root node. This method will also create the node if it could not be found within the graph.
	 *
	 * @return
	 */
	public MeshRoot meshRoot() {
		if (meshRoot == null) {
			synchronized (BootstrapInitializer.class) {
				// Check reference graph and finally create the node when it can't be found.
				Iterator<? extends MeshRootImpl> it = db.getElementsForType(MeshRootImpl.class);
				if (it.hasNext()) {
					isInitialSetup = false;
					meshRoot = it.next();
				} else {
					meshRoot = GraphDBTx.getGraphTx().getGraph().addFramedVertex(MeshRootImpl.class);
					if (log.isDebugEnabled()) {
						log.debug("Created mesh root {" + meshRoot.getUuid() + "}");
					}
				}
			}
		}
		return meshRoot;
	}

	@Override
	public void syncIndex() {
		SYNC_INDEX_ACTION.invoke();
	}

	@Override
	public void markChangelogApplied() {
		log.info("This is the initial setup.. marking all found changelog entries as applied");
		changelogSystem.markAllAsApplied();
		highlevelChangelogSystem.markAllAsApplied(meshRoot.getChangelogRoot());
		log.info("All changes marked");
	}

	@Override
	protected void initStandalone(MeshOptions options, PostProcessFlags flags, boolean isInitMode) throws Exception {
		if (options instanceof OrientDBMeshOptions) {
			GraphStorageOptions storageOptions = ((OrientDBMeshOptions)options).getStorageOptions();
			boolean startOrientServer = storageOptions != null && storageOptions.getStartServer();

			if (startOrientServer) {
				db.clusterManager().startAndSync();
			}
        }
	}

	// TODO: make change log generalized
	@Override
	public void invokeChangelog(PostProcessFlags flags) {

		log.info("Invoking database changelog check...");
		ChangelogSystem cls = new ChangelogSystemImpl(db, options);
		if (!cls.applyChanges(flags)) {
			throw new RuntimeException("The changelog could not be applied successfully. See log above.");
		}

		// Update graph indices and vertex types (This may take some time)
		DatabaseHelper.init(db);

		// Now run the high level changelog entries
		highlevelChangelogSystem.apply(flags, meshRoot.getChangelogRoot(), null);

		log.info("Changelog completed.");
		cls.setCurrentVersionAndRev();
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

		// wait for writeQuorum, then raise a global lock and execute changelog
		db.clusterManager().waitUntilWriteQuorumReached()
				.andThen(doWithLock(executeChangelog(flags, configuration), 60 * 1000)).subscribe();
	}

	/**
	 * Return a completable which will try to get a global lock with name {@link #GLOBAL_CHANGELOG_LOCK_KEY} and will then execute the locked action
	 * @param lockedAction locked action
	 * @param timeout timeout for waiting for the lock
	 * @return completable
	 */
	protected Completable doWithLock(Completable lockedAction, long timeout) {
		return mesh.getRxVertx().sharedData().rxGetLockWithTimeout(GLOBAL_CHANGELOG_LOCK_KEY, timeout).toMaybe()
				.flatMapCompletable(lock -> {
					log.debug("Acquired lock for executing changelog");
					return lockedAction.doFinally(() -> {
						log.debug("Releasing lock for executing changelog");
						lock.release();
					});
				});
	}

	/**
	 * Completable which will execute the highlevel changelog and will also store the (new) mesh version and DB revision to the DB
	 * @param flags
	 * @param configuration
	 * @return
	 */
	protected Completable executeChangelog(PostProcessFlags flags, MeshOptions configuration) {
		return Completable.defer(() -> {
			// Now run the high level changelog entries, which are allowed to be executed in cluase mode
			highlevelChangelogSystem.apply(flags, meshRoot.getChangelogRoot(), filter -> filter.isAllowedInCluster(configuration));

			log.info("Changelog completed.");
			new ChangelogSystemImpl(db, options).setCurrentVersionAndRev();
			return Completable.complete();
		});
	}

	@Override
	public boolean requiresChangelog(Predicate<? super HighLevelChange> filter) {
		log.info("Checking whether changelog entries need to be applied");
		ChangelogSystem cls = new ChangelogSystemImpl(db, options);
		return cls.requiresChanges() || highlevelChangelogSystem.requiresChanges(meshRoot.getChangelogRoot(), filter);
	}
}
