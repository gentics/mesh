package com.gentics.mesh.hibernate.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.hibernate.util.HibernateUtil.firstOrNull;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingContainerDao;
import com.gentics.mesh.core.data.dao.RootDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.domain.HibAddFieldChangeImpl;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibDatabaseElement;
import com.gentics.mesh.hibernate.data.domain.HibFieldTypeChangeImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.HibRemoveFieldChangeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUpdateFieldChangeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUpdateMicroschemaChangeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUpdateSchemaChangeImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityManager;

/**
 * Partial implementation of common parts for entity container DAOs.
 * 
 * @author plyhun
 *
 * @param <R> the type on entity container
 * @param <RM> the type of entity container version
 * @param <RE> the item entity reference type
 * @param <SC> the item entity type
 * @param <SCV> the item entity version type
 * @param <M> the container model
 * @param <D> the end implementation type of item entity
 * @param <DV> the end implementation type of item entity version
 */
public abstract class AbstractHibContainerDao<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
			M extends FieldSchemaContainer, 
			D extends SC, 
			DV extends SCV
		> extends AbstractHibDaoGlobal<SC, R, D> implements PersistingContainerDao<R, RM, RE, SC, SCV, M>, RootDao<HibProject, SC> {

	protected final DaoHelper<SCV, DV> versionDaoHelper;
	private final RootDaoHelper<SC, D, HibProject, HibProjectImpl> rootDaoHelper;

	public AbstractHibContainerDao(RootDaoHelper<SC, D, HibProject, HibProjectImpl> rootDaoHelper, HibPermissionRoots permissionRoots,
								   CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory,
								   DaoHelper<SCV, DV> versionDaoHelper, Lazy<Vertx> vertx) {
		super(rootDaoHelper.getDaoHelper(), permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.versionDaoHelper = versionDaoHelper;
		this.rootDaoHelper = rootDaoHelper;
	}

	@Override
	public HibSchemaChange<?> createPersistedChange(SCV version, SchemaChangeOperation schemaChangeOperation) {
		HibSchemaChange<?> schemaChange = null;
		switch (schemaChangeOperation) {
		case ADDFIELD:
			schemaChange = CommonTx.get().create(HibAddFieldChangeImpl.class);
			break;
		case REMOVEFIELD:
			schemaChange = CommonTx.get().create(HibRemoveFieldChangeImpl.class);
			break;
		case UPDATEFIELD:
			schemaChange = CommonTx.get().create(HibUpdateFieldChangeImpl.class);
			break;
		case CHANGEFIELDTYPE:
			schemaChange = CommonTx.get().create(HibFieldTypeChangeImpl.class);
			break;
		case UPDATESCHEMA:
			schemaChange = CommonTx.get().create(HibUpdateSchemaChangeImpl.class);
			break;
		case UPDATEMICROSCHEMA:
			schemaChange = CommonTx.get().create(HibUpdateMicroschemaChangeImpl.class);
			break;
		default:
			throw error(BAD_REQUEST, "error_change_operation_unknown", String.valueOf(schemaChangeOperation));
		}
		return schemaChange;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<SCV> findAllVersions(SC schema) {
		EntityManager em = currentTransaction.getEntityManager();
		return (List<SCV>) em.createQuery("select v" +
					" from " + getVersionFieldLabel() + "version v " +
					" join v." + getVersionFieldLabel() + " s" +
					" where s = :schema",
				getVersionPersistenceClass())
			.setParameter("schema", schema)
			.getResultList();
	}

	@Override
	public Result<SCV> findActiveSchemaVersions(HibBranch branch) {
		EntityManager em = currentTransaction.getEntityManager();
		return new TraversalResult<>(em.createQuery(
					"select e.version" +
					" from branch_" + getVersionFieldLabel() + "_version_edge e" +
					" where e.branch = :branch" +
					" and e.active = true",
				getVersionPersistenceClass())
			.setParameter("branch", branch)
			.getResultList());
	}

	@Override
	public SCV findVersionByRev(SC schema, String version) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.createQuery("select v" +
					" from " + getVersionFieldLabel() + "version v" +
					" join v." + getVersionFieldLabel() + " s" +
					" where s = :schema" + 
					" and v.version = :version",
				getVersionPersistenceClass())
			.setParameter("schema", schema)
			.setParameter("version", version)
			.setMaxResults(1)
			.getResultStream()
			.findAny()
			.orElse(null);
	}

	@Override
	public SCV findVersionByUuid(SC schema, String versionUuid) {
		return findVersionByUuid(schema, UUIDUtil.toJavaUuid(versionUuid));
	}

	@Override
	public boolean contains(HibProject project, SC schema) {
		return rootDaoHelper.findByElementInRoot(project, "dbUuid", ((HibDatabaseElement) schema).getDbUuid()).findAny().isPresent();
	}

	@Override
	public long globalCount(HibProject root) {
		return rootDaoHelper.countInRoot(root);
	}

	@Override
	public Stream<? extends SC> findAllStream(HibProject root, InternalActionContext ac,
			InternalPermission permission, PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		return StreamSupport.stream(rootDaoHelper.findAllInRoot(root, ac, paging, maybeFilter, null, permission).spliterator(), false);
	}

	@Override
	public long countAll(HibProject root, InternalActionContext ac, InternalPermission permission,
			PagingParameters pagingInfo, Optional<FilterOperation<?>> maybeFilter) {
		return rootDaoHelper.countAllInRoot(root, ac, pagingInfo, maybeFilter, permission);
	}

	@Override
	public Page<? extends SC> findAll(HibProject root, InternalActionContext ac, PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), null, true);
	}

	@Override
	public Page<? extends SC> findAll(HibProject root, InternalActionContext ac, PagingParameters pagingInfo,
			Predicate<SC> extraFilter) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), extraFilter, true);
	}

	@Override
	public Page<? extends SC> findAllNoPerm(HibProject root, InternalActionContext ac,
			PagingParameters pagingInfo) {
		return rootDaoHelper.findAllInRoot(root, ac, pagingInfo, Optional.empty(), null, false);
	}

	@Override
	public SC findByName(HibProject root, String name) {
		return firstOrNull(rootDaoHelper.findByElementInRoot(root, null, "name", name, null));
	}

	@Override
	public SC findByName(HibProject root, InternalActionContext ac, String name, InternalPermission perm) {
		return firstOrNull(rootDaoHelper.findByElementInRoot(root, ac, "name", name, perm));
	}

	@Override
	public SC findByUuid(HibProject root, String uuid) {
		if (!UUIDUtil.isUUID(uuid)) {
			return null;
		}
		return findByUuid(root, UUIDUtil.toJavaUuid(uuid));
	}

	//TODO getVersionFieldLabel() does not necessarily match, consider adding another label getter.
	@Override
	public Result<? extends HibBranch> getBranches(SCV version) {
		EntityManager em = em();
		return new TraversalResult<>(em.createQuery(
						"select distinct e.branch" +
								" from branch_" + getVersionFieldLabel() + "_version_edge e" +
								" where e.version = :version",
						HibBranchImpl.class)
				.setParameter("version", version)
				.getResultList());
	}

	/**
	 * Find the container by UUID in the project
	 * 
	 * @param root
	 * @param uuid
	 * @return
	 */
	public SC findByUuid(HibProject root, UUID uuid) {
		return firstOrNull(rootDaoHelper.findByElementInRoot(root, null, "dbUuid", uuid, null));
	}

	/**
	 * Find the container version by the UUID in the project
	 * 
	 * @param schema
	 * @param versionUuid
	 * @return
	 */
	public SCV findVersionByUuid(SC schema, UUID versionUuid) {
		EntityManager em = currentTransaction.getEntityManager();
		return em.createQuery("select v" +
					" from " + getVersionTableLabel() + " v" +
					" join v." + getVersionFieldLabel() + " s" +
					" where s = :schema" + 
					" and v.dbUuid = :uuid",
				getVersionPersistenceClass())
			.setParameter("schema", schema)
			.setParameter("uuid", versionUuid)
			.setMaxResults(1)
			.getResultStream()
			.findAny()
			.orElse(null);
	}

	/**
	 * Get the field label for the container version.
	 * 
	 * @return
	 */
	protected abstract String getVersionFieldLabel();

	/**
	 * Get the table label for the container version.
	 * 
	 * @return
	 */
	protected abstract String getVersionTableLabel();
}
