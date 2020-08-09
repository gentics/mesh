package com.syncleus.ferma.ext.orientdb3;

import static com.gentics.mesh.core.graph.GraphAttribute.MESH_COMPONENT;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.madl.traversal.RawTraversalResultImpl;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.db.AbstractTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.cluster.TxCleanupTask;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import io.micrometer.core.instrument.Timer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBTx extends AbstractTx<FramedTransactionalGraph> {

	private static final Logger log = LoggerFactory.getLogger(OrientDBTx.class);

	boolean isWrapped = false;

	private final TypeResolver typeResolver;

	private final Timer commitTimer;
	private final Database db;
	private final BootstrapInitializer boot;
	private final TxData txData;

	public OrientDBTx(MeshOptions options, Database db, BootstrapInitializer boot, DaoCollection daos, OrientStorage provider, TypeResolver typeResolver, Timer commitTimer) {
		this.db = db;
		this.boot = boot;
		this.typeResolver = typeResolver;
		this.commitTimer = commitTimer;
		// Check if an active transaction already exists.
		Tx activeTx = Tx.get();
		if (activeTx != null) {
			isWrapped = true;
			init(activeTx.getGraph());
		} else {
			DelegatingFramedOrientGraph transaction = new DelegatingFramedOrientGraph((OrientGraph) provider.rawTx(), typeResolver);
			init(transaction);
		}
		this.txData = new OrientTxData(options, daos);
	}

	@Override
	public void close() {
		try {
			if (isSuccess()) {
				try {
					db.blockingTopologyLockCheck();
					Thread t = Thread.currentThread();
					TxCleanupTask.register(t);
					Timer.Sample sample = Timer.start();
					try {
						commit();
					} finally {
						sample.stop(commitTimer);
						TxCleanupTask.unregister(t);
					}
				} catch (Exception e) {
					rollback();
					throw e;
				}
			} else {
				rollback();
			}

		} catch (ONeedRetryException e) {
			throw e;
		} finally {
			if (!isWrapped) {
				// Restore the old graph that was previously swapped with the current graph
				getGraph().shutdown();
				Tx.setActive(null);
			}
		}
	}

	@Override
	public <T extends RawTraversalResult<?>> T traversal(Function<GraphTraversalSource, GraphTraversal<?, ?>> traverser) {
		return (T) new RawTraversalResultImpl(traverser.apply(rawTraverse()), typeResolver);
	}

	@Override
	public GraphTraversalSource rawTraverse() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T createVertex(Class<T> clazzOfR) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <E extends Element> E getElement(Object id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int txId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void init(FramedTransactionalGraph transactionalGraph) {
		Mesh mesh = boot.mesh();
		if (mesh != null) {
			transactionalGraph.setAttribute(MESH_COMPONENT, mesh.internal());
		} else {
			log.error("Could not set mesh component attribute. Followup errors may happen.");
		}
		super.init(transactionalGraph);
	}

	@Override
	public TxData data() {
		return txData;
	}
}
