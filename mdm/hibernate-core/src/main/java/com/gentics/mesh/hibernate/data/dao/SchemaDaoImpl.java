package com.gentics.mesh.hibernate.data.dao;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.IteratorUtils;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
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

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.TypedQuery;

/**
 * Schema DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class SchemaDaoImpl 
		extends AbstractHibContainerDao<SchemaResponse, SchemaVersionModel, SchemaReference, Schema, SchemaVersion, SchemaModel, HibSchemaImpl, HibSchemaVersionImpl> 
		implements PersistingSchemaDao {

	private final ContentStorage contentStorage;
	private final DatabaseConnector databaseConnector;

	@Inject
	public SchemaDaoImpl(RootDaoHelper<Schema, HibSchemaImpl, Project, HibProjectImpl> rootDaoHelper, HibPermissionRoots permissionRoots,
			CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, EventFactory eventFactory, DatabaseConnector databaseConnector,
			DaoHelper<SchemaVersion, HibSchemaVersionImpl> versionDaoHelper, Lazy<Vertx> vertx, ContentStorage contentStorage) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, versionDaoHelper, vertx);
		this.contentStorage = contentStorage;
		this.databaseConnector = databaseConnector;
	}

	@Override
	public Schema create(SchemaVersionModel schema, User creator, String uuid, boolean validate) {
		Schema container = PersistingSchemaDao.super.create(schema, creator, uuid, validate);
		SchemaVersion version = container.getLatestVersion();

		currentTransaction.getEntityManager().persist(version);
		currentTransaction.getEntityManager().persist(container);
		return container;
	}

	@Override
	public Result<? extends Schema> findAll(Project root) {
		return new TraversalResult<>(root.getSchemas());
	}

	@Override
	public void addItem(Project root, Schema item) {
		((HibProjectImpl) root).getHibSchemas().add(item);
		((HibSchemaImpl) item).getProjects().add(root);
		em().merge(root);
	}

	@Override
	public void removeItem(Project root, Schema item) {
		((HibProjectImpl) root).getHibSchemas().remove(item);
		((HibSchemaImpl) item).getProjects().remove(root);
		em().merge(root);
	}

	@Override
	public Result<? extends Node> getNodes(Schema schema) {
		Stream<HibNodeImpl> nodes = em().createNamedQuery("node.findBySchema", HibNodeImpl.class)
				.setParameter("schema", schema)
				.getResultStream();

		return new TraversalResult<>(nodes.iterator());
	}

	@Override
	public Result<Project> findLinkedProjects(Schema schema) {
		return new TraversalResult<>(((HibSchemaImpl) schema).getLinkedProjects());
	}

	@Override
	public Result<? extends NodeFieldContainer> findDraftFieldContainers(SchemaVersion version,
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
	public Result<? extends Node> findNodes(SchemaVersion version, String branchUuid, User user,
			ContainerType type) {
		Schema schema = version.getSchemaContainer();

		if (user.isAdmin()) {
			return new TraversalResult<>(em().createNamedQuery("node.findBySchemaBranchTypeForAdmin", HibNodeImpl.class)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.setParameter("schemaUuid", schema.getId()).getResultList());
		} else {
			List<Role> roles = IteratorUtils.toList(Tx.get().userDao().getRoles(user).iterator());
			return new TraversalResult<>(SplittingUtils.splitAndMergeInList(roles, HibernateUtil.inQueriesLimitForSplitting(3), slice -> em().createNamedQuery("node.findBySchemaBranchType", HibNodeImpl.class)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.setParameter("type", type)
					.setParameter("schemaUuid", schema.getId())
					.setParameter("roles", slice)
					.getResultList()));
		}
	}

	@Override
	public Stream<? extends NodeFieldContainer> getFieldContainers(SchemaVersion version, String branchUuid) {
		List<HibNodeFieldContainerEdgeImpl> edges = em().createNamedQuery("contentEdge.findByBranchAndVersion", HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("versionUuid", version.getId())
				.getResultList();

		List<HibNodeFieldContainerImpl> fieldContainers = contentStorage.findMany(edges);

		return fieldContainers.stream();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<? extends NodeFieldContainer> getFieldContainers(SchemaVersion version, String branchUuid,
			Bucket bucket) {

		String nativeQuery = "select distinct edge.* from " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "nodefieldcontainer edge "
				+ " inner join " + databaseConnector.getPhysicalTableName(version) + " content "
						+ " on content." + databaseConnector.renderColumn(CommonContentColumn.DB_UUID) + " = edge." + databaseConnector.renderNonContentColumn("contentUuid")
						+ " and content." + databaseConnector.renderColumn(CommonContentColumn.SCHEMA_VERSION_DB_UUID) + " = edge." + databaseConnector.renderNonContentColumn("version_dbUuid")
				+ " where edge." + databaseConnector.renderNonContentColumn("branch_dbUuid") + " = :branchUuid "
				+ " and edge." + databaseConnector.renderNonContentColumn("version_dbUuid") + " = :versionUuid "
				+ " and content." + databaseConnector.renderColumn(CommonContentColumn.BUCKET_ID) + " >= :bucketStart "
				+ " and content." + databaseConnector.renderColumn(CommonContentColumn.BUCKET_ID) + " <= :bucketEnd ";

		List<HibNodeFieldContainerEdgeImpl> edges = em().createNativeQuery(nativeQuery, HibNodeFieldContainerEdgeImpl.class)
				.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
				.setParameter("versionUuid", version.getId())
				.setParameter("bucketStart", bucket.start())
				.setParameter("bucketEnd", bucket.end())
				.getResultList();

		return contentStorage.findMany(edges).stream();
	}

	@Override
	public Class<? extends SchemaVersion> getVersionPersistenceClass() {
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
	public Schema beforeDeletedFromDatabase(Schema element) {
		HibSchemaImpl schema = (HibSchemaImpl) element;
		schema.getVersions().forEach(v -> v.setSchemaContainer(null));
		schema.getVersions().clear();
		schema.setLatestVersion(null);
		schema.getProjects().forEach(p -> ((HibProjectImpl) p).getHibSchemas().remove(schema));
		schema.getProjects().clear();
		return schema;
	}

	@Override
	public SchemaVersion createPersistedVersion(Schema container, Consumer<SchemaVersion> inflater) {
		SchemaVersion version = super.createPersistedVersion(container, inflater);
		HibernateTx.get().contentDao().createContentTable(version);
		return version;
	}

	@Override
	public void beforeVersionDeletedFromDatabase(SchemaVersion version) {
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
}
