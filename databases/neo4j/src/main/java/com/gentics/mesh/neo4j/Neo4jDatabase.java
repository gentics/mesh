package com.gentics.mesh.neo4j;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.neo4j.kernel.internal.Version;

import com.gentics.madl.tx.Tx;
import com.gentics.madl.tx.TxAction;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ServerRole;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.cluster.ClusterManager;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.neo4j.tx.Neo4jTx;
import com.gentics.mesh.neo4j.type.Neo4jTypeResolver;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import dagger.Lazy;
import io.vertx.core.Vertx;
import scala.NotImplementedError;

@Singleton
public class Neo4jDatabase extends AbstractDatabase {

	private MetricsService metrics;
	private Neo4jTypeHandler typeHandler;
	private Neo4jIndexHandler indexHandler;
	private Neo4jStorage txProvider;
	private Neo4jClusterManager clusterManager;
	private TypeResolver resolver;

	@Inject
	public Neo4jDatabase(Lazy<Vertx> vertx, MetricsService metrics, Neo4jTypeHandler typeHandler, Neo4jIndexHandler indexHandler,
		Neo4jClusterManager clusterManager) {
		super(vertx);
		this.metrics = metrics;
		this.typeHandler = typeHandler;
		this.indexHandler = indexHandler;
		this.clusterManager = clusterManager;
	}

	@Override
	public void init(MeshOptions options, String meshVersion, String... basePaths) throws Exception {
		super.init(options, meshVersion);
		resolver = new Neo4jTypeResolver(basePaths);
	}

	@Override
	public void setupConnectionPool() throws Exception {
		initGraphDB();
	}

	private void initGraphDB() {
		txProvider = new Neo4jStorage(options, metrics);
	}

	@Override
	public Neo4jTypeHandler type() {
		return typeHandler;
	}

	@Override
	public Neo4jIndexHandler index() {
		return indexHandler;
	}

	@Override
	public ClusterManager clusterManager() {
		return clusterManager;
	}

	@Override
	public String backupGraph(String backupDirectory) throws IOException {
		throw new NotImplementedError("Not yet supported for Neo4j");
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		throw new NotImplementedError("Not yet supported for Neo4j");
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		throw new NotImplementedError("Not yet supported for Neo4j");
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		throw new NotImplementedError("Not yet supported for Neo4j");
	}

	@Override
	public void closeConnectionPool() {
		txProvider.close();
	}

	@Override
	public void enableMassInsert() {
		// NOOP
	}

	@Override
	public String getVersion() {
		return Version.getNeo4jVersion();
	}

	@Override
	public String getVendorName() {
		return "Neo4j";
	}

	@Override
	public void reload(Element element) {
		// NOOP
	}

	@Override
	public void setMassInsertIntent() {
		// NOOP
	}

	@Override
	public void resetIntent() {
		// NOOP
	}

	@Override
	public void reload(MeshElement element) {
		// NOOP
	}

	@Override
	public void stop() {
		if (txProvider != null) {
			txProvider.close();
		}
		clusterManager.stop();
		Tx.setActive(null);
	}

	@Override
	public List<String> getChangeUuidList() {
		return ChangesList.getList(options).stream().map(c -> c.getUuid()).collect(Collectors.toList());
	}

	@Override
	public void shutdown() {
		txProvider.close();
	}

	@Override
	public String getElementVersion(Element element) {
		// TODO retrieve version via a dedicated property of the element
		return element.getProperty("Version");
	}

	@Override
	public <T extends MeshVertex> Iterator<? extends T> getVerticesForType(Class<T> classOfVertex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends EdgeFrame> T findEdge(String propertyKey, Object propertyValue, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends MeshElement> T findVertex(String propertyKey, Object propertyValue, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends VertexFrame> TraversalResult<T> getVerticesTraversal(Class<T> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TransactionalGraph rawTx() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tx tx() {
		return new Neo4jTx(txProvider, resolver);
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		try (Tx tx = tx()) {
			T handlerResult = txHandler.handle(tx);
			tx.success();
			return handlerResult;
		} catch (Exception e) {
			throw new RuntimeException("Transaction error", e);
		}
	}

	@Override
	public ClusterConfigResponse loadClusterConfig() {
		return null;
	}

	@Override
	public void updateClusterConfig(ClusterConfigRequest request) {

	}


	@Override
	public void blockingTopologyLockCheck() {

	}

	@Override
	public void setToMaster() {

	}

}
