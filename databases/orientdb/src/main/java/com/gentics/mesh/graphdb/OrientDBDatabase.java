package com.gentics.mesh.graphdb;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.metric.Metrics.TX_RETRY;
import static com.gentics.mesh.metric.Metrics.TX_TIME;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.index.OrientDBIndexHandler;
import com.gentics.mesh.graphdb.index.OrientDBTypeHandler;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.gentics.mesh.graphdb.tx.impl.OrientLocalStorageImpl;
import com.gentics.mesh.graphdb.tx.impl.OrientServerStorageImpl;
import com.gentics.mesh.metric.Metrics;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.ETag;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.ext.orientdb3.OrientDBTx;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.tx.TxAction;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;
import com.tinkerpop.pipes.util.FastNoSuchElementException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB specific mesh graph database implementation.
 */
@Singleton
public class OrientDBDatabase extends AbstractDatabase {

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private TypeResolver resolver;

	private int maxRetry = 10;

	private OrientStorage txProvider;

	private MetricsService metrics;

	private Timer txTimer;

	private Counter txRetryCounter;

	private OrientDBIndexHandler indexHandler;

	private OrientDBTypeHandler typeHandler;

	private OrientDBClusterManager clusterManager;

	@Inject
	public OrientDBDatabase(MetricsService metrics, OrientDBTypeHandler typeHandler, OrientDBIndexHandler indexHandler,
		OrientDBClusterManager clusterManager) {
		this.metrics = metrics;
		if (metrics != null) {
			txTimer = metrics.timer(TX_TIME);
			txRetryCounter = metrics.counter(TX_RETRY);
		}
		this.typeHandler = typeHandler;
		this.indexHandler = indexHandler;
		this.clusterManager = clusterManager;
	}

	@Override
	public void stop() {
		// // TODO let other nodes know we are stopping the instance?
		// if (options.getClusterOptions().isEnabled()) {
		// Mesh.vertx().eventBus().publish(MeshEvent.CLUSTER_NODE_LEAVING, new JsonObject().put("node", getNodeName()));
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		if (txProvider != null) {
			txProvider.close();
		}
		clusterManager.stop();
		Tx.setActive(null);
	}

	@Override
	public void clear() {
		txProvider.clear();
	}

	@Override
	public void init(MeshOptions options, String meshVersion, String... basePaths) throws Exception {
		super.init(options, meshVersion);

		GraphStorageOptions storageOptions = options.getStorageOptions();
		boolean startOrientServer = storageOptions != null && storageOptions.getStartServer();
		boolean isInMemory = storageOptions.getDirectory() == null;

		if (isInMemory && startOrientServer) {
			throw new RuntimeException(
				"Using the graph database server is only possible for non-in-memory databases. You have not specified a graph database directory.");
		}

		OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);

		clusterManager.initConfigurationFiles();

		// resolver = new OrientDBTypeResolver(basePaths);
		resolver = new MeshTypeResolver(basePaths);
		if (options != null && storageOptions.getParameters() != null && storageOptions.getParameters().get("maxTransactionRetry") != null) {
			this.maxRetry = Integer.valueOf(storageOptions.getParameters().get("maxTransactionRetry"));
			log.info("Using {" + this.maxRetry + "} transaction retries before failing");
		}
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
	public OrientGraph rawTx() {
		return txProvider.rawTx();
	}

	protected OrientGraphNoTx rawNoTx() {
		return txProvider.rawNoTx();
	}

	/**
	 * Start the orientdb related process. This will also setup the graph connection pool and handle clustering.
	 */
	@Override
	public void setupConnectionPool() throws Exception {
		Orient.instance().startup();
		initGraphDB();
	}

	/**
	 * Setup the OrientDB Graph connection
	 */
	private void initGraphDB() {
		if (clusterManager.isServerActive()) {
			txProvider = new OrientServerStorageImpl(options, clusterManager.getServer().getContext(), metrics);
		} else {
			txProvider = new OrientLocalStorageImpl(options, metrics);
		}
		// Open the storage
		txProvider.open();
	}

	@Override
	public void closeConnectionPool() {
		txProvider.close();
	}

	@Override
	public void shutdown() {
		Orient.instance().shutdown();
	}

	@Override
	public Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		return orientBaseGraph.getVertices(classOfVertex.getSimpleName(), fieldNames, fieldValues).iterator();
	}

	@Override
	public <T extends MeshVertex> Iterator<? extends T> getVerticesForType(Class<T> classOfVertex) {
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		FramedGraph fermaGraph = Tx.getActive().getGraph();
		Iterator<Vertex> rawIt = orientBaseGraph.getVertices("@class", classOfVertex.getSimpleName()).iterator();
		return fermaGraph.frameExplicit(rawIt, classOfVertex);
	}

	/**
	 * Unwrap the current thread local graph.
	 * 
	 * @return
	 */
	public OrientBaseGraph unwrapCurrentGraph() {
		FramedGraph graph = Tx.getActive().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph tx = ((OrientBaseGraph) baseGraph);
		return tx;
	}

	@Override
	public void enableMassInsert() {
		OrientBaseGraph tx = unwrapCurrentGraph();
		tx.getRawGraph().getTransaction().setUsingLog(false);
		tx.declareIntent(new OIntentMassiveInsert().setDisableHooks(true).setDisableValidation(true));
	}

	@Override
	public <T extends MeshElement> T findVertex(String fieldKey, Object fieldValue, Class<T> clazz) {
		FramedGraph graph = Tx.getActive().getGraph();
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		Iterator<Vertex> it = orientBaseGraph.getVertices(clazz.getSimpleName(), new String[] { fieldKey }, new Object[] { fieldValue }).iterator();
		if (it.hasNext()) {
			return graph.frameNewElementExplicit(it.next(), clazz);
		}
		return null;
	}

	@Override
	public <T extends EdgeFrame> T findEdge(String fieldKey, Object fieldValue, Class<T> clazz) {
		FramedGraph graph = Tx.getActive().getGraph();
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		Iterator<Edge> it = orientBaseGraph.getEdges(fieldKey, fieldValue).iterator();
		if (it.hasNext()) {
			return graph.frameNewElementExplicit(it.next(), clazz);
		}
		return null;
	}

	@Override
	public void reload(MeshElement element) {
		reload(element.getElement());
	}

	@Override
	public void reload(Element element) {
		if (element instanceof OrientElement) {
			if (metrics.isEnabled()) {
				metrics.meter(Metrics.GRAPH_ELEMENT_RELOAD).mark();
			}
			((OrientElement) element).reload();
		}
	}

	/**
	 * @deprecated Don't use tx method directly. Use {@link #tx(com.syncleus.ferma.tx.TxAction0)} instead to avoid tx commit issues.
	 */
	@Override
	@Deprecated
	public Tx tx() {
		return new OrientDBTx(txProvider, resolver);
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		/**
		 * OrientDB uses the MVCC pattern which requires a retry of the code that manipulates the graph in cases where for example an
		 * {@link OConcurrentModificationException} is thrown.
		 */
		T handlerResult = null;
		boolean handlerFinished = false;
		for (int retry = 0; retry < maxRetry; retry++) {
			final Timer.Context context = txTimer.time();
			try (Tx tx = tx()) {
				handlerResult = txHandler.handle(tx);
				handlerFinished = true;
				tx.success();
			} catch (OSchemaException e) {
				log.error("OrientDB schema exception detected.");
				// TODO maybe we should invoke a metadata getschema reload?
				// factory.getTx().getRawGraph().getMetadata().getSchema().reload();
				// Database.getThreadLocalGraph().getMetadata().getSchema().reload();
			} catch (ONeedRetryException | FastNoSuchElementException e) {
				if (log.isTraceEnabled()) {
					log.trace("Error while handling transaction. Retrying " + retry, e);
				}
				int rnd = (int) (Math.random() * 6000.0);
				try {
					// Increase the delay for each retry by 25ms to give the other transaction a chance to finish
					Thread.sleep(50 + (retry * 25) + rnd);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				// Reset previous result
				handlerFinished = false;
				handlerResult = null;
			} catch (ORecordDuplicatedException e) {
				log.error(e);
				throw error(INTERNAL_SERVER_ERROR, "error_internal");
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
				context.stop();
			}
			if (!handlerFinished && log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
				if (metrics.isEnabled()) {
					txRetryCounter.inc();
				}
			}
			if (handlerFinished) {
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit {" + maxRetry + "} for trx exceeded");
	}

	@Override
	public void backupGraph(String backupDirectory) throws IOException {
		txProvider.backup(backupDirectory);
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		txProvider.restore(backupFile);
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		txProvider.exportGraph(outputDirectory);
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		txProvider.importGraph(importFile);
	}

	@Override
	public String getElementVersion(Element element) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex) element).getBaseElement();
		}
		OrientElement e = (OrientElement) element;
		String uuid = element.getProperty("uuid");
		return ETag.hash(uuid + e.getRecord().getVersion());
	}

	@Override
	public String getVendorName() {
		return "orientdb";
	}

	@Override
	public String getVersion() {
		return OConstants.getVersion();
	}

	@Override
	public OrientDBTypeHandler type() {
		return typeHandler;
	}

	@Override
	public OrientDBIndexHandler index() {
		return indexHandler;
	}

	public OrientStorage getTxProvider() {
		return txProvider;
	}

	public OrientDBClusterManager clusterManager() {
		return clusterManager;
	}

	@Override
	public List<String> getChangeUuidList() {
		return ChangesList.getList().stream().map(c -> c.getUuid()).collect(Collectors.toList());
	}

}
