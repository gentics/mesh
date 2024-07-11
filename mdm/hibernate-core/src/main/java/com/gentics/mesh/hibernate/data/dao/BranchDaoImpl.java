package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.jpa.QueryHints;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.branch.BranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.BranchSchemaVersion;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibBranchMicroschemaVersionEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBranchSchemaVersionEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibJobImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.TypedQuery;

/**
 * Branch DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class BranchDaoImpl extends AbstractHibRootDao<Branch, BranchResponse, HibBranchImpl, Project, HibProjectImpl> implements PersistingBranchDao {

	@Inject
	public BranchDaoImpl(RootDaoHelper<Branch, HibBranchImpl, Project, HibProjectImpl> rootDaoHelper,
						 HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper,
						 CurrentTransaction currentTransaction, EventFactory eventFactory,
						 Lazy<Vertx> vertx) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public Branch createPersisted(Project root, String uuid, Consumer<Branch> inflater) {
		HibBranchImpl branch = daoHelper.create(uuid, b -> {
			b.setProject(root);
			inflater.accept(b);
		});
		((HibProjectImpl) root).addBranch(branch);
		return afterCreatedInDatabase(branch);
	}

	@Override
	public void onRootDeleted(Project root, BulkActionContext bac) {
		// before deleting all branches of the project, unset the initial and latest branch in the project
		HibProjectImpl hibProject = (HibProjectImpl) root;
		hibProject.setInitialBranch(null);
		hibProject.setLatestBranch(null);

		// also unset the "previousBranch" relation of all branches
		findAll(root).forEach(branch -> branch.setPreviousBranch(null));

		super.onRootDeleted(root, bac);
	}

	@Override
	public void deletePersisted(Project root, Branch entity) {
		em().createQuery("select j from job j where j.branch = :branch", HibJobImpl.class)
				.setParameter("branch", entity)
				.getResultStream()
				.forEach(job -> job.setBranch(null));
		em().remove(entity);
	}

	@Override
	public Result<? extends Branch> findAll(Project project) {
		Stream<HibBranchImpl> branches = em().createNamedQuery("branch.findFromProject", HibBranchImpl.class)
				.setParameter("project", project)
				.getResultStream();

		return new TraversalResult<>(branches.iterator());
	}

	@Override
	public Page<? extends Branch> findAll(Project project, InternalActionContext ac, PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), null, true);
	}

	@Override
	public Page<? extends Branch> findAll(Project project, InternalActionContext ac, PagingParameters pagingInfo, Predicate<Branch> extraFilter) {
		return rootDaoHelper.findAllInRoot(project, ac, pagingInfo, Optional.empty(), extraFilter, true);
	}

	@Override
	public Function<Branch, Project> rootGetter() {
		return Branch::getProject;
	}

	@Override
	public Branch findByName(Project project, String name) {
		return HibernateTx.get().data().mesh().branchCache().get(getCacheKey(project, name), key -> {
			return firstOrNull(
					em().createQuery("select b from branch b where b.name = :name and b.project = :project", HibBranchImpl.class)
							.setParameter("name", name)
							.setParameter("project", project)
					.setHint(QueryHints.HINT_CACHEABLE, true)
			);
		});
	}

	@Override
	public Branch findConflictingBranch(Branch branch, String name) {
		return daoHelper.findByName(name);
	}

	@Override
	public String getAPIPath(Branch branch, InternalActionContext ac) {
		return branch.getAPIPath(ac);
	}

	@Override
	public BranchSchemaVersion connectToSchemaVersion(Branch branch, SchemaVersion version) {
		HibBranchSchemaVersionEdgeImpl schemaVersion = ((HibBranchImpl) branch).addSchemaVersion((HibSchemaVersionImpl) version);
		em().persist(schemaVersion);
		return schemaVersion;
	}

	@Override
	public BranchMicroschemaVersion connectToMicroschemaVersion(Branch branch, MicroschemaVersion version) {
		HibBranchMicroschemaVersionEdgeImpl microschemaVersion = ((HibBranchImpl) branch).addMicroschemaVersion((HibMicroschemaVersionImpl) version);
		em().persist(microschemaVersion);
		return microschemaVersion;
	}

	@Override
	public Branch getLatestBranch(Project project) {
		return project.getLatestBranch();
	}

	@Override
	public Stream<? extends Branch> findAllStream(Project root, InternalActionContext ac,
													 InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		// TODO FIXME this fix belongs to PersistingTagDao
		if (paging == null) {
			paging = ac.getPagingParameters();
		}
		return StreamSupport.stream(rootDaoHelper.findAllInRoot(root, ac, paging, maybeFilter, null, permission).spliterator(), false);
	}

	@Override
	public long countAll(Project root, InternalActionContext ac, InternalPermission permission,
			PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		// TODO FIXME this fix belongs to PersistingTagDao
		if (paging == null) {
			paging = ac.getPagingParameters();
		}
		return rootDaoHelper.countAllInRoot(root, ac, paging, maybeFilter, permission);
	}

	@Override
	public Page<? extends Branch> findAllNoPerm(Project root, InternalActionContext ac,
												   PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), null, false);
	}

	@Override
	public void addItem(Project root, Branch item) {
		((HibProjectImpl) root).addBranch(item);
	}

	@Override
	public void removeItem(Project root, Branch item) {
		((HibProjectImpl) root).removeBranch(item);
	}

	@Override
	public Class<? extends Branch> getPersistenceClass(Project root) {
		return HibBranchImpl.class;
	}

	@Override
	public Branch mergeIntoPersisted(Project root, Branch entity) {
		return em().merge(entity);
	}

	@Override
	public BranchSchemaVersion findBranchSchemaEdge(Branch branch, SchemaVersion schemaVersion) {
		TypedQuery<HibBranchSchemaVersionEdgeImpl> query = em().createQuery("select v from branch b " +
								"join b.schemaVersions v " +
								"where b = :branch and v.version.dbUuid = :schemaVersionDbUuid",
						HibBranchSchemaVersionEdgeImpl.class)
				.setParameter("branch", branch)
				.setParameter("schemaVersionDbUuid", UUIDUtil.toJavaUuid(schemaVersion.getUuid()));

		return firstOrNull(query.getResultStream());
	}

	@Override
	public BranchMicroschemaVersion findBranchMicroschemaEdge(Branch branch, MicroschemaVersion microschemaVersion) {
		TypedQuery<HibBranchMicroschemaVersionEdgeImpl> query = em().createQuery("select v from branch b " +
								"join b.microschemaVersions v " +
								"where b = :branch and v.version.dbUuid = :microSchemaVersionDbUuid",
						HibBranchMicroschemaVersionEdgeImpl.class)
				.setParameter("branch", branch)
				.setParameter("microSchemaVersionDbUuid", UUIDUtil.toJavaUuid(microschemaVersion.getUuid()));

		return firstOrNull(query.getResultStream());
	}

	@Override
	public Branch getInitialBranch(Project project) {
		return ((HibProjectImpl)project).getInitialBranch();
	}
}
