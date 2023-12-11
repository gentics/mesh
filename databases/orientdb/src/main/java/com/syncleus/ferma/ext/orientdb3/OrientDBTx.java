package com.syncleus.ferma.ext.orientdb3;

import static com.gentics.mesh.core.graph.GraphAttribute.MESH_COMPONENT;
import static com.gentics.mesh.metric.SimpleMetric.COMMIT_TIME;

import javax.inject.Inject;

import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.ImageVariantDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBDaoCollection;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.S3BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.db.AbstractTx;
import com.gentics.mesh.core.db.CommonTxData;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.cluster.TxCleanupTask;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.security.SecurityUtils;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import dagger.Lazy;
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
	private final CommonTxData txData;
	private final ContextDataRegistry contextDataRegistry;
	private final OrientDBDaoCollection daos;
	/**
	 * We provide a lazy instance, otherwise we risk prematurely subscribing to the event bus in certain bootstrapping
	 * scenarios (mesh clustered + init cluster flag set to true)
	 */
	private final Lazy<CacheCollection> caches;
	private final SecurityUtils security;
	private final Binaries binaries;
	private final S3Binaries s3binaries;

	private Timer commitTimer;

	@Inject
	public OrientDBTx(OrientDBMeshOptions options, Database db, OrientDBBootstrapInitializer boot,
					  OrientDBDaoCollection daos, Lazy<CacheCollection> caches, SecurityUtils security, OrientStorage provider,
					  TypeResolver typeResolver, MetricsService metrics, PermissionRoots permissionRoots,
					  ContextDataRegistry contextDataRegistry, S3Binaries s3binaries, Binaries binaries, CommonTxData txData) {
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
		this.txData = txData;
		this.contextDataRegistry = contextDataRegistry;
		this.daos = daos;
		this.caches = caches;
		this.security = security;
		this.binaries = binaries;
		this.s3binaries = s3binaries;
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
	public CommonTxData data() {
		return txData;
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
	public UserDaoWrapper userDao() {
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
	public GroupDaoWrapper groupDao() {
		return daos.groupDao();
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return daos.roleDao();
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return daos.projectDao();
	}

	@Override
	public JobDaoWrapper jobDao() {
		return daos.jobDao();
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return daos.languageDao();
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return daos.schemaDao();
	}

	@Override
	public TagDaoWrapper tagDao() {
		return daos.tagDao();
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return daos.tagFamilyDao();
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return daos.microschemaDao();
	}

	@Override
	public BinaryDaoWrapper binaryDao() {
		return daos.binaryDao();
	}

	@Override
	public S3BinaryDaoWrapper s3binaryDao() {
		return daos.s3binaryDao();
	}

	@Override
	public BranchDaoWrapper branchDao() {
		return daos.branchDao();
	}

	@Override
	public NodeDaoWrapper nodeDao() {
		return daos.nodeDao();
	}

	@Override
	public ContentDaoWrapper contentDao() {
		return daos.contentDao();
	}

	@Override
	public ImageVariantDaoWrapper imageVariantDao() {
		return daos.imageVariantDao();
	}

	@Override
	public Binaries binaries() {
		return binaries;
	}

	@Override
	public S3Binaries s3binaries() {
		return s3binaries;
	}

	@Override
	public PermissionCache permissionCache() {
		return caches.get().permissionCache();
	}

	@Override
	public PasswordEncoder passwordEncoder() {
		return security.passwordEncoder();
	}
}
