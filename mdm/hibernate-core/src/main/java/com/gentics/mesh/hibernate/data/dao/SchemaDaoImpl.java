package com.gentics.mesh.hibernate.data.dao;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.IteratorUtils;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/**
 * Schema DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class SchemaDaoImpl 
		extends AbstractHibContainerDao<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion, SchemaModel, HibSchemaImpl, HibSchemaVersionImpl> 
		implements PersistingSchemaDao {

	private final ContentStorage contentStorage;
	private final DatabaseConnector databaseConnector;

	@Inject
	public SchemaDaoImpl(RootDaoHelper<HibSchema, HibSchemaImpl, HibProject, HibProjectImpl> rootDaoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory, DatabaseConnector databaseConnector,
			DaoHelper<HibSchemaVersion, HibSchemaVersionImpl> versionDaoHelper, Lazy<Vertx> vertx, ContentStorage contentStorage) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, versionDaoHelper, vertx);
		this.contentStorage = contentStorage;
		this.databaseConnector = databaseConnector;
	}

	@Override
	public HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid, boolean validate) {
		HibSchema container = PersistingSchemaDao.super.create(schema, creator, uuid, validate);
		HibSchemaVersion version = container.getLatestVersion();

		currentTransaction.getEntityManager().persist(version);
		currentTransaction.getEntityManager().persist(container);
		return container;
	}

	@Override
	public Result<? extends HibSchema> findAll(HibProject root) {
		return new TraversalResult<>(root.getSchemas());
	}

	@Override
	public void addItem(HibProject root, HibSchema item) {
		((HibProjectImpl) root).getHibSchemas().add(item);
		((HibSchemaImpl) item).getProjects().add(root);
		em().merge(root);
	}

	@Override
	public void removeItem(HibProject root, HibSchema item) {
		((HibProjectImpl) root).getHibSchemas().remove(item);
		((HibSchemaImpl) item).getProjects().remove(root);
		em().merge(root);
	}

	@Override
	public Result<? extends HibNode> getNodes(HibSchema schema) {
		Stream<HibNodeImpl> nodes = em().createNamedQuery("node.findBySchema", HibNodeImpl.class)
				.setParameter("schema", schema)
				.getResultStream();

		return new TraversalResult<>(nodes.iterator());
	}

	@Override
	public Result<HibProject> findLinkedProjects(HibSchema schema) {
		return new TraversalResult<>(((HibSchemaImpl) schema).getLinkedProjects());
	}

	@Override
	public Result<? extends HibNodeFieldContainer> findDraftFieldContainers(HibSchemaVersion version,
			String branchUuid, long limit) {
		TypedQuery<HibNodeFieldContainerEdgeImpl> query = em().createNamedQuery("contentEdge.findByBranchVersionAndType", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("versionUuid", version.getId())
				.setParameter("type", ContainerType.DRAFT);
		if (limit > 0) {
			query = query.setMaxResults((int) limit);
		}
		List<HibNodeFieldContainerEdgeImpl> edges = query.getResultList();
		List<HibNodeFieldContainerImpl> fieldContainers = contentStorage.findMany(edges);
		return new TraversalResult<>(fieldContainers);
	}

	@Override
	public Result<? extends HibNode> findNodes(HibSchemaVersion version, String branchUuid, HibUser user,
			ContainerType type) {
		HibSchema schema = version.getSchemaContainer();

		if (user.isAdmin()) {
			return new TraversalResult<>(em().createNamedQuery("node.findBySchemaBranchTypeForAdmin", HibNodeImpl.class)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.setParameter("schemaUuid", schema.getId()).getResultList());
		} else {
			List<HibRole> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			return new TraversalResult<>(SplittingUtils.splitAndMergeInList(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createNamedQuery("node.findBySchemaBranchType", HibNodeImpl.class)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.setParameter("schemaUuid", schema.getId())
					.setParameter("roles", slice)
					.getResultList()));
		}
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid) {
		List<HibNodeFieldContainerEdgeImpl> edges = em().createNamedQuery("contentEdge.findByBranchAndVersion", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("versionUuid", version.getId())
				.getResultList();

		List<HibNodeFieldContainerImpl> fieldContainers = contentStorage.findMany(edges);

		return fieldContainers.stream();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<? extends HibNodeFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid,
			Bucket bucket, Optional<ContainerType> optType) {

		String nativeQuery = "select distinct edge.* from " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "nodefieldcontainer edge "
				+ " inner join " + databaseConnector.getPhysicalTableName(version) + " content "
						+ " on content." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID) + " = edge." + databaseConnector.renderNonContentColumn("contentUuid")
						+ " and content." + databaseConnector.renderColumn(CommonContentColumn.SCHEMA_VERSION_DB_UUID) + " = edge." + databaseConnector.renderNonContentColumn("version_dbUuid")
				+ " where edge." + databaseConnector.renderNonContentColumn("branch_dbUuid") + " = :branchUuid "
				+ " and edge." + databaseConnector.renderNonContentColumn("version_dbUuid") + " = :versionUuid "
				+ " and content." + databaseConnector.renderColumn(CommonContentColumn.BUCKET_ID) + " >= :bucketStart "
				+ " and content." + databaseConnector.renderColumn(CommonContentColumn.BUCKET_ID) + " <= :bucketEnd ";
		if (optType.isPresent()) {
			nativeQuery += " and edge." + databaseConnector.renderNonContentColumn("type") + " = :type ";
		}

		Query query = em().createNativeQuery(nativeQuery, HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("versionUuid", version.getId())
				.setParameter("bucketStart", bucket.start())
				.setParameter("bucketEnd", bucket.end());

		if (optType.isPresent()) {
			query = query.setParameter("type", optType.get().name());
		}

		List<HibNodeFieldContainerEdgeImpl> edges = query
				.getResultList();

		return contentStorage.findMany(edges).stream();
	}

	@Override
	public Class<? extends HibSchemaVersion> getVersionPersistenceClass() {
		return HibSchemaVersionImpl.class;
	}

	@Override
	protected String getVersionFieldLabel() {
		return "schema";
	}

	@Override
	protected String getVersionTableLabel() {
		return HibSchemaVersionImpl.TABLE_NAME;
	}

	@Override
	public HibSchema beforeDeletedFromDatabase(HibSchema element) {
		HibSchemaImpl schema = (HibSchemaImpl) element;
		schema.getVersions().forEach(v -> v.setSchemaContainer(null));
		schema.getVersions().clear();
		schema.setLatestVersion(null);
		schema.getProjects().forEach(p -> ((HibProjectImpl) p).getHibSchemas().remove(schema));
		schema.getProjects().clear();
		return schema;
	}

	@Override
	public HibSchemaVersion createPersistedVersion(HibSchema container, Consumer<HibSchemaVersion> inflater) {
		HibSchemaVersion version = super.createPersistedVersion(container, inflater);
		HibernateTx.get().contentDao().createContentTable(version);
		return version;
	}

	@Override
	public void beforeVersionDeletedFromDatabase(HibSchemaVersion version) {
		HibernateTx tx = HibernateTx.get();

		// Drop references from the branches
		tx.projectDao().findAll().stream()
				.flatMap(project -> tx.branchDao().findAll(project).stream())
				.map(HibBranchImpl.class::cast)
				.forEach(branch -> branch.removeSchemaVersion(version));

		// Rearrange versioning
		HibSchemaImpl schema = (HibSchemaImpl) version.getSchemaContainer();
		schema.getVersions().removeIf(v -> v.getUuid().equals(version.getUuid()));
		if (schema.getLatestVersion().getUuid().equals(version.getUuid())) {
			schema.setLatestVersion(version.getPreviousVersion());
		}
		if (version.getPreviousVersion() != null) {
			version.getPreviousVersion().setNextVersion(null);
		}
		if (version.getNextVersion() != null) {
			version.getNextVersion().setPreviousVersion(version.getPreviousVersion());
		}

		// Drop the whole content table
		tx.contentDao().deleteContentTable(version);

		// dorop schema versions
		em().createQuery("delete from branch_schema_version_edge bsve where bsve.version = :version")
				.setParameter("version", version)
				.executeUpdate();
	}

	@Override
	public HibSchemaVersion findLatestVersion(HibBranch branch, HibSchema schema) {
		List<HibSchemaVersionImpl> versions = em().createNamedQuery("schemaversion.findInBranchForSchema", HibSchemaVersionImpl.class)
			.setParameter("branch", branch)
			.setParameter("schema", schema)
			.getResultList();

		return versions.stream().sorted((v1, v2) -> VersionUtil.compareVersions(v2.getVersion(), v1.getVersion()))
				.findFirst().orElse(null);
	}
}
