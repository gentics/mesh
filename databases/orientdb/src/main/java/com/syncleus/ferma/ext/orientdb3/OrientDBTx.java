package com.syncleus.ferma.ext.orientdb3;

import static com.gentics.mesh.core.graph.GraphAttribute.MESH_COMPONENT;
import static com.gentics.mesh.metric.SimpleMetric.COMMIT_TIME;

import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.madl.traversal.RawTraversalResultImpl;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cache.CacheCollection;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.context.ContextDataRegistry;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.AbstractTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.cluster.TxCleanupTask;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.security.SecurityUtils;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import io.micrometer.core.instrument.Timer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of an OrientDB transaction.
 * 
 * This implementation cares care of various aspects including:
 * <ul>
 * <li>Handling of nested/wrapped transactions</li>
 * <li>Collecting metrics on Tx duration</li>
 * <li>Providing access to various dao methods</li>
 * <li>Providing access to {@link TxData} via {@link #txData}</li>
 * <li>Handling topology locks before commit via {@link Database#blockingTopologyLockCheck()}</li>
 * <li>Register and Unregister of active Tx via {@link TxCleanupTask}</li>
 * <li>Making Tx accessible via threadlocal {@link Tx#setActive(Tx)}
 * </ul>
 */
public class OrientDBTx extends AbstractTx<FramedTransactionalGraph> {

	private static final Logger log = LoggerFactory.getLogger(OrientDBTx.class);

	boolean isWrapped = false;

	private final TypeResolver typeResolver;

	private final Database db;
	private final BootstrapInitializer boot;
	private final TxData txData;
	private final ContextDataRegistry contextDataRegistry;
	private final DaoCollection daos;
	private final CacheCollection caches;
	private final SecurityUtils security;
	private final Binaries binaries;

	private Timer commitTimer;

	@Inject
	public OrientDBTx(OrientDBMeshOptions options, Database db, OrientDBBootstrapInitializer boot, 
		DaoCollection daos, CacheCollection caches, SecurityUtils security, OrientStorage provider,
		TypeResolver typeResolver, MetricsService metrics, PermissionRoots permissionRoots, 
		ContextDataRegistry contextDataRegistry, NodeIndexHandler nodeIndexHandler, 
		WebRootLinkReplacer webRootLinkReplacer, Binaries binaries) {
		this.db = db;
		this.boot = boot;
		this.typeResolver = typeResolver;
		if (metrics != null) {
			this.commitTimer = metrics.timer(COMMIT_TIME);
		}
		// Check if an active transaction already exists.
		GraphDBTx activeTx = GraphDBTx.getGraphTx();
		if (activeTx != null) {
			// TODO Use this spot here to check for nested / wrapped transactions. Nested Tx must not be used when using MDM / Hibernate
			isWrapped = true;
			init(activeTx.getGraph());
		} else {
			DelegatingFramedOrientGraph transaction = new DelegatingFramedOrientGraph((OrientGraph) provider.rawTx(), typeResolver);
			init(transaction);
		}
		this.txData = new TxDataImpl(options, boot, permissionRoots, nodeIndexHandler, null);
		this.contextDataRegistry = contextDataRegistry;
		this.daos = daos;
		this.caches = caches;
		this.security = security;
		this.binaries = binaries;
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

	@Override
	public HibBranch getBranch(InternalActionContext ac) {
		return contextDataRegistry.getBranch(ac);
	}

	@Override
	public HibBranch getBranch(InternalActionContext ac, HibProject project) {
		return contextDataRegistry.getBranch(ac, project);
	}

	@Override
	public HibProject getProject(InternalActionContext ac) {
		return contextDataRegistry.getProject(ac);
	}

	// DAOs

	@Override
	public UserDao userDao() {
		return daos.userDao();
	}

	@Override
	public UserDAOActions userActions() {
		return daos.userActions();
	}

	@Override
	public GroupDAOActions groupActions() {
		return daos.groupActions();
	}

	@Override
	public RoleDAOActions roleActions() {
		return daos.roleActions();
	}

	@Override
	public ProjectDAOActions projectActions() {
		return daos.projectActions();
	}

	@Override
	public TagFamilyDAOActions tagFamilyActions() {
		return daos.tagFamilyActions();
	}

	@Override
	public TagDAOActions tagActions() {
		return daos.tagActions();
	}

	@Override
	public BranchDAOActions branchActions() {
		return daos.branchActions();
	}

	@Override
	public MicroschemaDAOActions microschemaActions() {
		return daos.microschemaActions();
	}

	@Override
	public SchemaDAOActions schemaActions() {
		return daos.schemaActions();
	}

	@Override
	public GroupDao groupDao() {
		return daos.groupDao();
	}

	@Override
	public RoleDao roleDao() {
		return daos.roleDao();
	}

	@Override
	public ProjectDao projectDao() {
		return daos.projectDao();
	}

	@Override
	public JobDao jobDao() {
		return daos.jobDao();
	}

	@Override
	public LanguageDao languageDao() {
		return daos.languageDao();
	}

	@Override
	public SchemaDao schemaDao() {
		return daos.schemaDao();
	}

	@Override
	public TagDao tagDao() {
		return daos.tagDao();
	}

	@Override
	public TagFamilyDao tagFamilyDao() {
		return daos.tagFamilyDao();
	}

	@Override
	public MicroschemaDao microschemaDao() {
		return daos.microschemaDao();
	}

	@Override
	public BinaryDao binaryDao() {
		return daos.binaryDao();
	}

	@Override
	public BranchDao branchDao() {
		return daos.branchDao();
	}

	@Override
	public NodeDao nodeDao() {
		return daos.nodeDao();
	}

	@Override
	public ContentDao contentDao() {
		return daos.contentDao();
	}

	@Override
	public Binaries binaries() {
		return binaries;
	}

	@Override
	public PermissionCache permissionCache() {
		return caches.permissionCache();
	}

	@Override
	public PasswordEncoder passwordEncoder() {
		return security.passwordEncoder();
	}

}
