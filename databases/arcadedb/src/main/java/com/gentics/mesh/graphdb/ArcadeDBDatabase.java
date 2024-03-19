package com.gentics.mesh.graphdb;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.tinkerpop.gremlin.process.traversal.util.FastNoSuchElementException;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;

import com.arcadedb.Constants;
import com.arcadedb.ContextConfiguration;
import com.arcadedb.GlobalConfiguration;
import com.arcadedb.database.Database;
import com.arcadedb.exception.NeedRetryException;
import com.arcadedb.exception.SchemaException;
import com.arcadedb.gremlin.ArcadeElement;
import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.server.ArcadeDBServer;
import com.gentics.madl.ext.arcadedb.DelegatingFramedArcadeGraph;
import com.gentics.madl.graph.DelegatingFramedMadlGraph;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.core.db.TxAction0;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ClusterServerConfig;
import com.gentics.mesh.core.rest.admin.cluster.ServerRole;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphDBMeshOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.check.DiskQuotaChecker;
import com.gentics.mesh.graphdb.cluster.TxCleanupTask;
import com.gentics.mesh.graphdb.dagger.TransactionComponent;
import com.gentics.mesh.graphdb.index.ArcadeDBIndexHandler;
import com.gentics.mesh.graphdb.index.ArcadeDBTypeHandler;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.query.MeshArcadeGraphVertexQuery;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.tx.ArcadeStorage;
import com.gentics.mesh.graphdb.tx.impl.ArcadeServerStorageImpl;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.SimpleMetric;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.FramedGraph;

import dagger.Lazy;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * ArcadeDB specific mesh graph database implementation.
 */
@Singleton
public class ArcadeDBDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(ArcadeDBDatabase.class);

	private static final String RIDBAG_PARAM_KEY = "ridBag.embeddedToSbtreeBonsaiThreshold";

	private static final String DISK_QUOTA_CHECKER_THREAD_NAME = "mesh-disk-quota-checker";

	private MeshTypeResolver resolver;

	private ArcadeStorage txProvider;

	private ArcadeDBIndexHandler indexHandler;

	private ArcadeDBTypeHandler typeHandler;

	private ClusterManager clusterManager;

	private final TxCleanupTask txCleanUpTask;

	private Thread txCleanupThread;

	private WriteLock writeLock;

	private final TransactionComponent.Factory txFactory;

	private final ArcadeDBServer server;

	/**
	 * Executor service for running the disk quota check
	 */
	private ScheduledExecutorService diskQuotaCheckerService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, DISK_QUOTA_CHECKER_THREAD_NAME);
		}
	});

	/**
	 * scheduled disk quota check
	 */
	private ScheduledFuture<?> diskQuotaChecker;

	/**
	 * Local disk-quota-exceeded status (will be set to "true" if the local disk gets full)
	 */
	private boolean diskQuotaExceeded = false;

	/**
	 * Long Gauge Metric for the total disk space
	 */
	private AtomicLong totalDiskSpace;

	/**
	 * Long Gauge Metric for usable disk space
	 */
	private AtomicLong usableDiskSpace;

	@Inject
	public ArcadeDBDatabase(
			GraphDBMeshOptions options, Lazy<Vertx> vertx, Lazy<BootstrapInitializer> boot, 
			Lazy<DaoCollection> daos, MetricsService metrics,
			ArcadeDBTypeHandler typeHandler, ArcadeDBIndexHandler indexHandler,
			ClusterManager clusterManager, TxCleanupTask txCleanupTask,
			Lazy<PermissionRoots> permissionRoots, WriteLock writeLock,
			TransactionComponent.Factory txFactory, Mesh mesh
	) {
		super(vertx, mesh, metrics);
		this.options = options;

		if (options != null) {
			GlobalConfiguration.SERVER_ROOT_PASSWORD.setValue(options.getInitialAdminPassword());
			GlobalConfiguration.SERVER_DATABASE_DIRECTORY.setValue(options.getStorageOptions().getDirectory());
		}

		if (metrics != null) {
			totalDiskSpace = metrics.longGauge(ArcadeDBStorageMetric.DISK_TOTAL);
			usableDiskSpace = metrics.longGauge(ArcadeDBStorageMetric.DISK_USABLE);
		}
		this.typeHandler = typeHandler;
		this.indexHandler = indexHandler;
		this.clusterManager = clusterManager;
		this.txCleanUpTask = txCleanupTask;
		this.writeLock = writeLock;
		this.txFactory = txFactory;

		ContextConfiguration config = new ContextConfiguration();
		this.server = new ArcadeDBServer(config);
	}

	@Override
	public void stop() {
		txCleanUpTask.interruptActive();
		Tx.setActive(null);
		if (txCleanupThread != null) {
			log.info("Stopping tx cleanup thread");
			txCleanupThread.interrupt();
		}

		//clusterManager.stop();

		if (txProvider != null) {
			txProvider.close();
		}
	}

	@Override
	public void clear() {
		txProvider.clear();
	}

	@Override
	public void init(String meshVersion, String... basePaths) throws Exception {
		super.init(meshVersion);

		GraphStorageOptions storageOptions = options.getStorageOptions();
		boolean startArcadeServer = storageOptions != null && storageOptions.getStartServer();
		boolean isInMemory = storageOptions.getDirectory() == null;

		if (isInMemory && startArcadeServer) {
			throw new RuntimeException(
				"Using the graph database server is only possible for non-in-memory databases. You have not specified a graph database directory.");
		}

		int value = getRidBagValue(options);
		if (log.isTraceEnabled()) {
			log.trace("Using ridbag transition threshold {" + value + "}");
		}
		//GlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(value);
		//GlobalConfiguration.WARNING_DEFAULT_USERS.setValue(false);

		clusterManager.initConfigurationFiles();

		// resolver = new ArcadeDBTypeResolver(basePaths);
		resolver = new MeshTypeResolver(basePaths);

		if (storageOptions.getTxCommitTimeout() != 0) {
			startTxCleanupTask();
		}

		startDiskQuotaChecker();
	}

	/**
	 * Load the ridbag configuration setting.
	 *
	 * @param options
	 * @return
	 */
	private int getRidBagValue(GraphDBMeshOptions options) {
		boolean isClusterMode = options.getClusterOptions() != null && options.getClusterOptions().isEnabled();
		if (isClusterMode) {
			// This is the mandatory setting when using ArcadeDB in clustered mode.
			return Integer.MAX_VALUE;
		} else {
			GraphStorageOptions storageOptions = options.getStorageOptions();
			String val = storageOptions.getParameters().get(RIDBAG_PARAM_KEY);
			if (val != null) {
				try {
					return Integer.parseInt(val);
				} catch (Exception e) {
					log.error("Could not parse value of storage parameter {" + RIDBAG_PARAM_KEY + "}");
					throw new RuntimeException("Parameter {" + RIDBAG_PARAM_KEY + "} could not be parsed.");
				}
			}

		}
		// Default instead of 40 to avoid sudden changes in sort order
		return Integer.MAX_VALUE;
	}

	@Override
	public void setMassInsertIntent() {
		txProvider.setMassInsertIntent();
	}

	@Override
	public void resetIntent() {
		txProvider.resetIntent();
	}

	@Override
	public ArcadeGraph rawTx() {
		return txProvider.rawTx();
	}

	protected Database rawNoTx() {
		return txProvider.rawNoTx();
	}

	/**
	 * Start the arcadedb related process. This will also setup the graph connection pool and handle clustering.
	 */
	@Override
	public void setupConnectionPool() throws Exception {
		startDiskQuotaChecker();
		server.start();
		initGraphDB();
	}

	/**
	 * Setup the ArcadeDB Graph connection
	 */
	private void initGraphDB() {
		// TODO what if we don't need local at all?
		//if (options.getClusterOptions().isEnabled()) {
			txProvider = new ArcadeServerStorageImpl(options, server, metrics);
		//} else {
		//	txProvider = new ArcadeLocalStorageImpl(options, metrics);
		//}
		// Open the storage
		txProvider.open();
	}

	@Override
	public void closeConnectionPool() {
		txProvider.close();
	}

	@Override
	public void shutdown() {
		stopDiskQuotaChecker();
		server.stop();
	}

	@Override
	public Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues, PagingParameters paging, Optional<ContainerType> maybeContainerType, Optional<String> maybeFilter) {
		DelegatingFramedMadlGraph<? extends Graph> madlGraph = GraphDBTx.getGraphTx().getGraph();
		MeshArcadeGraphVertexQuery query = new MeshArcadeGraphVertexQuery((ArcadeGraph) madlGraph.getBaseGraph(), classOfVertex);
		query.relationDirection(Direction.OUT);
		query.hasAll(fieldNames, fieldValues);
		query.filter(maybeFilter);
		if (PersistingRootDao.shouldPage(paging)) {
			query.skip((int) (paging.getActualPage() * paging.getPerPage()));
			query.limit(paging.getPerPage().intValue());
		}
		String[] sorted;
		if (PersistingRootDao.shouldSort(paging)) {
			List<String> sortParams = paging.getSort().entrySet().stream().map(e -> e.getKey() + " " + e.getValue().getValue()).collect(Collectors.toUnmodifiableList());
			sorted = sortParams.toArray(new String[sortParams.size()]);
		} else {
			sorted = new String[0];
		}
		query.setOrderPropsAndDirs(sorted);
		Iterator<Vertex> ret = query.fetch(maybeContainerType).iterator();
		return ret;
	}

	@Override
	public Iterable<Vertex> getVerticesForRange(Class<?> classOfVertex, String indexPostfix, String[] fieldNames, Object[] fieldValues, String rangeKey, long start, long end) {
		ArcadeGraph arcadeBaseGraph = unwrapCurrentGraph();
		MeshArcadeGraphVertexQuery query = new MeshArcadeGraphVertexQuery(arcadeBaseGraph, classOfVertex);
		query.hasAll(fieldNames, fieldValues).skip(start).limit(start + end);
		return query.fetch(Optional.empty());
	}

	@Override
	public <T extends VertexFrame> Result<T> getVerticesTraversal(Class<T> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		Stream<Vertex> stream = toStream(getVertices(classOfVertex, fieldNames, fieldValues));
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();

		return new TraversalResult<>(stream.map(v -> {
			return graph.frameElementExplicit(v, classOfVertex);
		}));
	}

	@Override
	public <T extends HibElement> Iterator<? extends T> getElementsForType(Class<T> classOfVertex) {
		Graph arcadeBaseGraph = unwrapCurrentGraph();
		FramedGraph fermaGraph = GraphDBTx.getGraphTx().getGraph();
		Iterator<Vertex> rawIt = arcadeBaseGraph.traversal().V().hasLabel(classOfVertex.getSimpleName());
		return fermaGraph.frameExplicit(rawIt, classOfVertex);
	}

	/**
	 * Unwrap the current thread local graph.
	 *
	 * @return
	 */
	public ArcadeGraph unwrapCurrentGraph() {
		DelegatingFramedMadlGraph<? extends Graph> graph = GraphDBTx.getGraphTx().getGraph();
		ArcadeGraph baseGraph = ((DelegatingFramedArcadeGraph) graph).getBaseGraph();
		return baseGraph;
	}

	@Override
	public void enableMassInsert() {
//		ArcadeGraph tx = unwrapCurrentGraph();
//		tx.getDatabase().getTransaction().setUsingLog(false);
//		tx.getRawDatabase().declareIntent(new OIntentMassiveInsert().setDisableHooks(true).setDisableValidation(true));
	}

	@Override
	public <T extends MeshElement> T findVertex(String fieldKey, Object fieldValue, Class<T> clazz) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph arcadeBaseGraph = unwrapCurrentGraph();
		Iterator<Vertex> it = arcadeBaseGraph.vertices(clazz.getSimpleName(), new String[] { fieldKey }, new Object[] { fieldValue });
		if (it.hasNext()) {
			return graph.frameNewElementExplicit(it.next(), clazz);
		}
		return null;
	}

	@Override
	public long count(Class<? extends HibBaseElement> clazz) {
		ArcadeGraph arcadeBaseGraph = unwrapCurrentGraph();
		long count = 0;
		boolean noSuchClass;
		try {
			count = arcadeBaseGraph.getDatabase().countType(clazz.getSimpleName(), false);
			noSuchClass = false;
		} catch (Exception e) {
			log.warn("No class [" + clazz + "] found for count", e);
			noSuchClass = true;
		}
		if (noSuchClass) {
			try {
				count = arcadeBaseGraph.getDatabase().countType(clazz.getSimpleName() + "Impl", false);
				noSuchClass = false;
			} catch (Exception e) {
				log.warn("No class [" + clazz + "Impl] found for count", e);
				noSuchClass = true;
			}
		}
		return count;
	}

	@Override
	public <T extends EdgeFrame> T findEdge(String fieldKey, Object fieldValue, Class<T> clazz) {
		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		Graph arcadeBaseGraph = unwrapCurrentGraph();
		Iterator<Edge> it = arcadeBaseGraph.edges(fieldKey, fieldValue);
		if (it.hasNext()) {
			return graph.frameNewElementExplicit(it.next(), clazz);
		}
		return null;
	}

	@Override
	public void reload(HibElement element) {
		if (element instanceof ArcadeElement) {
			if (metrics.isEnabled()) {
				metrics.counter(SimpleMetric.GRAPH_ELEMENT_RELOAD).increment();
			}
			((ArcadeElement) element).getRecord().reload();
		}
	}

	/**
	 * @deprecated Don't use tx method directly. Use {@link #tx(TxAction0)} instead to avoid tx commit issues.
	 */
	@Override
	@Deprecated
	public GraphDBTx tx() {
		return txFactory.create(txProvider, resolver).tx();
	}

	@Override
	public void blockingTopologyLockCheck() {
		ClusterOptions clusterOptions = options.getClusterOptions();
		long lockTimeout = clusterOptions.getTopologyLockTimeout();
		if (clusterOptions.isEnabled() && clusterManager() != null && lockTimeout != 0) {
			long start = System.currentTimeMillis();
			long i = 0;
			Timer.Sample sample = Timer.start();
			while (clusterManager().isClusterTopologyLocked()) {
				long dur = System.currentTimeMillis() - start;
				if (i % 250 == 0) {
					log.info("Write operation locked due to topology lock. Locked since " + dur + "ms");
				}
				if (dur > lockTimeout) {
					topologyLockTimeoutCounter.increment();
					log.warn("Tx global lock timeout of {" + lockTimeout + "} reached.");
					break;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					log.error("Interrupting topology lock delay.", e);
					break;
				}
				i++;
			}
			sample.stop(this.topologyLockTimer);
		}
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		/**
		 * ArcadeDB uses the MVCC pattern which requires a retry of the code that manipulates the graph in cases where for example an
		 * {@link OConcurrentModificationException} is thrown.
		 */
		T handlerResult = null;
		boolean handlerFinished = false;
		int maxRetry = options.getStorageOptions().getTxRetryLimit();
		Throwable cause = null;
		Optional<EventQueueBatch> maybeBatch = Optional.empty();
		for (int retry = 0; retry < maxRetry; retry++) {
			Timer.Sample sample = Timer.start();
			// Check the status to prevent transactions during shutdown
			checkStatus();
			try (GraphDBTx tx = tx()) {
				handlerResult = txHandler.handle(tx);
				handlerFinished = true;
				tx.success();
				maybeBatch = tx.data().maybeGetEventQueueBatch();
			} catch (SchemaException e) {
				cause = e;
				log.error("ArcadeDB schema exception detected.");
				// TODO maybe we should invoke a metadata getschema reload?
				// factory.getTx().getRawGraph().getMetadata().getSchema().reload();
				// Database.getThreadLocalGraph().getMetadata().getSchema().reload();
			} catch (InterruptedException | NeedRetryException | FastNoSuchElementException e) {
				cause = e;
				if (log.isTraceEnabled()) {
					log.trace("Error while handling transaction. Retrying " + retry, e);
				}
				int delay = options.getStorageOptions().getTxRetryDelay();
				if (retry > 0 && delay > 0) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				// Reset previous result
				handlerFinished = false;
				handlerResult = null;
			} catch (GenericRestException e) {
				// Don't log. Just throw it along so that others can handle it
				throw e;
			} catch (RuntimeException e) {
				if (log.isDebugEnabled()) {
					log.debug("Error handling transaction", e);
				}
				throw e;
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Error handling transaction", e);
				}
				throw new RuntimeException("Transaction error", e);
			} finally {
				sample.stop(txTimer);
			}
			if (!handlerFinished && log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
				if (metrics.isEnabled()) {
					txRetryCounter.increment();
				}
			}
			if (handlerFinished) {
				maybeBatch.ifPresent(EventQueueBatch::dispatch);
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit {" + maxRetry + "} for trx exceeded", cause);
	}

	@Override
	public String backupDatabase(String backupDirectory) throws IOException {
		return txProvider.backup(backupDirectory);
	}

	@Override
	public void restoreDatabase(String backupFile) throws IOException {
		txProvider.restore(backupFile);
	}

	@Override
	public void exportDatabase(String outputDirectory) throws IOException {
		txProvider.exportGraph(outputDirectory);
	}

	@Override
	public void importDatabase(String importFile) throws IOException {
		txProvider.importGraph(importFile);
	}

	@Override
	public String getElementVersion(HibElement hibElement) {
		return getElementVersion((Element) hibElement);
	}

	@Override
	public String getElementVersion(Element element) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex<Element>) element).getBaseVertex();
		}
		ArcadeElement<?> e = (ArcadeElement<?>) element;
		String uuid = element.<String>property("uuid").orElse(null);
		return ETag.hash(uuid + e.getRecord().getIdentity());
	}

	@Override
	public String getVendorName() {
		return "arcadedb";
	}

	@Override
	public String getVersion() {
		return Constants.getVersion();
	}

	@Override
	public ArcadeDBTypeHandler type() {
		return typeHandler;
	}

	@Override
	public ArcadeDBIndexHandler index() {
		return indexHandler;
	}

	public ArcadeStorage getTxProvider() {
		return txProvider;
	}

	/**
	 * Return the arcadedb cluster manager.
	 */
	public ClusterManager clusterManager() {
		return clusterManager;
	}

	@Override
	public List<String> getChangeUuidList() {
		return ChangesList.getList(options).stream().map(c -> c.getUuid()).collect(Collectors.toList());
	}

	@Override
	public ClusterConfigResponse loadClusterConfig() {
		if (options.getClusterOptions().isEnabled() && clusterManager != null) {
			ClusterConfigResponse response = new ClusterConfigResponse();

			clusterManager.getVertxClusterManager().getNodes().forEach(node -> {
				ClusterServerConfig serverConfig = new ClusterServerConfig();
				serverConfig.setName(node);
				serverConfig.setRole(ServerRole.MASTER); // all mesh nodes can write/read with hibernate

				response.getServers().add(serverConfig);
			});

			// relational databases don't depend on mesh for reading/writing
			response.setReadQuorum(1);
			response.setWriteQuorum(String.valueOf(1));

			return response;
		} else {
			throw error(BAD_REQUEST, "error_cluster_status_only_available_in_cluster_mode");
		}
	}

	@Override
	public void setToMaster() {
		log.warn("setToMaster call is ignored");
	}

	@Override
	public void updateClusterConfig(ClusterConfigRequest request) {
		// nothing to do, in the context changing reading/writing quorum values or member roles
		// has no effect
		log.warn("update cluster configuration call was ignored");
	}

	private void startTxCleanupTask() {
		txCleanupThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				txCleanUpTask.checkTransactions();
				try {
					// Interval is fixed
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					log.info("Cleanup task stopped");
					break;
				}
			}
		});
		txCleanupThread.setName("mesh-tx-cleanup-task");
		txCleanupThread.start();
	}

	@Override
	public WriteLock writeLock() {
		return writeLock;
	}

	@Override
	public boolean isEmptyDatabase() {
		return tx(tx -> !toGraph(tx).getGraph().getBaseGraph().vertices().hasNext());
	}

	@Override
	public boolean isReadOnly(boolean logError) {
		if (diskQuotaExceeded) {
			if (logError) {
				log.error("Local instance is read-only due to limited disk space.");
			} else {
				log.warn("Local instance is read-only due to limited disk space.");
			}
			return true;
		}
		return false;
	}

	/**
	 * Start the disk quota checker, if configured to do so and not started before
	 */
	private void startDiskQuotaChecker() {
		if (diskQuotaChecker == null && options.getStorageOptions() != null
				&& options.getStorageOptions().getDirectory() != null
				&& options.getStorageOptions().getDiskQuotaOptions().getCheckInterval() > 0) {
			if (log.isDebugEnabled()) {
				log.debug("Starting disk quota checker");
			}
			diskQuotaChecker = diskQuotaCheckerService.scheduleAtFixedRate(
					new DiskQuotaChecker(new File(options.getStorageOptions().getDirectory()),
							options.getStorageOptions().getDiskQuotaOptions(), this::setDiskQuotaExceededStatus),
					0, options.getStorageOptions().getDiskQuotaOptions().getCheckInterval(), TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Set the disk-quota-exceeded status locally and in the cluster (if clustering is enabled)
	 * @param result result of the disk quota checker as triple of disk-quota-exceeded status, total space and usable space
	 */
	private void setDiskQuotaExceededStatus(Triple<Boolean, Long, Long> result) {
		this.diskQuotaExceeded = result.getLeft();
		if (this.totalDiskSpace != null) {
			this.totalDiskSpace.set(result.getMiddle());
	}
		if (this.usableDiskSpace != null) {
			this.usableDiskSpace.set(result.getRight());
		}
	}

	/**
	 * Stop the disk quota checker (if started before)
	 */
	private void stopDiskQuotaChecker() {
		if (diskQuotaChecker != null) {
			if (log.isDebugEnabled()) {
				log.debug("Stopping disk quota checker");
			}
			diskQuotaChecker.cancel(true);
			diskQuotaChecker = null;
		}
	}
}
