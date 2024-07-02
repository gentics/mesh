package com.gentics.mesh.enterprise.database.connector;

import static io.vertx.core.logging.LoggerFactory.getLogger;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.query.Query;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.Limit;
import org.hibernate.type.spi.TypeConfiguration;

import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.contentoperation.JoinedContentColumn;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.enterprise.database.HibernateTx;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.SessionMetadataIntegrator;
import com.gentics.mesh.enterprise.hibernate.data.node.field.HibListFieldEdge;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.query.MetadataExtractorIntegrator;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;

/**
 * Base functionality for the Mesh Database Connector
 * 
 * @param <DR>
 * @param <DI>
 */
public abstract class AbstractDatabaseConnector<DR extends Driver, DI extends Dialect> implements DatabaseConnector {

	protected static final String DELETE_BY_ELEMENT_IN = "delete from nodefieldcontainer where element in ";
	protected static final String FIND_BY_NODE_UUIDS = "select edge2.element from nodefieldcontainer edge2 where edge2.node.dbUuid in :nodesUuid";
	protected static final String FIND_BY_NODE_UUIDS_BRANCH = "select edge2.element from nodefieldcontainer edge2 where edge2.node.dbUuid in :nodesUuid and edge2.branch = :branch";
	protected static final String FIND_BY_PROJECT = "select edge2.element from nodefieldcontainer edge2 where edge2.node.project = :project";

	private static final Logger log = getLogger(AbstractDatabaseConnector.class);

	protected final MetadataExtractorIntegrator metadataExtractorIntegrator = new MetadataExtractorIntegrator();

	protected static Driver DRIVER = null;

	protected EnterpriseMeshOptions options;

	protected AbstractDatabaseConnector(EnterpriseMeshOptions options) {
		this.options = options;		
	}

	@SuppressWarnings("unchecked")
	@Override
	public DR getJdbcDriver() {
		// TODO Get the driver instance from Hibernate, if possible
		return instantiate((Class<DR>) getJdbcDriverClass(), () -> (DR) DRIVER);
	}

	@Override
	public SessionMetadataIntegrator getSessionMetadataIntegrator() {
		return metadataExtractorIntegrator;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return metadataExtractorIntegrator.getMetadata().getDatabase().getPhysicalNamingStrategy();
	}

	@Override
	public Class<? extends PhysicalNamingStrategy> getPhysicalNamingStrategyClass() {
		return MeshTablePrefixStrategy.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DI getHibernateDialect() {
		return metadataExtractorIntegrator.isInitialized() ? (DI) metadataExtractorIntegrator.getDialect() : instantiate((Class<DI>) getHibernateDialectClass(), () -> getHibernateDialect());
	}

	@Override
	public String getConnectorDescription() {
		return getJdbcDriverClass() + " / " + getHibernateDialectClass();
	}

	@Override
	public DatabaseConnector setOptions(EnterpriseMeshOptions options) {
		this.options = options;
		return this;
	}

	@Override
	public String installPaging(String query, String alias, PagingParameters pagingInfo) {
		LimitHandler limitHandler = getHibernateDialect().getLimitHandler();
		Limit limit = new Limit();
		limit.setFirstRow(pagingInfo.getActualPage() * pagingInfo.getPerPage().intValue());
		limit.setMaxRows(pagingInfo.getPerPage().intValue());
		query = limitHandler.processSql(query, limit, new QueryOptionsImpl());

		if (limitHandler instanceof AbstractLimitHandler && ((AbstractLimitHandler)limitHandler).bindLimitParametersInReverseOrder()) {
			return query.replaceFirst("\\?", " :limit ").replaceFirst("\\?", " :offset ");
		} else {
			return query.replaceFirst("\\?", " :offset ").replaceFirst("\\?", " :limit ");
		}
	}

	@Override
	public <T, Q extends Query<T>> Q installPagingArguments(Q query, PagingParameters pagingInfo) {
		String sqlQuery = query.getQueryString();
		if (sqlQuery.contains(" :" + getPagingLimitParamName() + " ")) {
			if (sqlQuery.contains(" :" + getPagingOffsetParamName() + " ")) {
				query.setParameter(getPagingOffsetParamName(), pagingInfo.getActualPage() * pagingInfo.getPerPage().intValue());
			}
			query.setParameter(getPagingLimitParamName(), pagingInfo.getPerPage());
		} else {
			if (sqlQuery.contains(" :" + getPagingOffsetParamName() + " ")) {
				query.setParameter(getPagingOffsetParamName(), pagingInfo.getPerPage());
			}
		}
		return query;
	}

	@Override
	public String installCount(String query) {
		return "SELECT COUNT(1) FROM ( " + query + " ) AS total";
	}

	@Override
	public String installStringContentColumn(String columnName, boolean makeAlias, boolean customAlias) {
		return columnName;
	}

	@Override
	public String installListContentColumn(String listItemType, String listAlias, String operator, String paramName) {
		return listAlias + "." + renderNonContentColumn("valueOrUuid") + " " + operator + " " + ":" + paramName;
	}

	@Override
	public String renderNonContentColumn(String column) {
		return renderColumnUnsafe(column, false);
	}

	@Override
	public String makeSortAlias(String column) {
		return column.replaceFirst("\\\"", "").replaceFirst("\\[", "").replaceFirst("\\{", "").replaceFirst("\\'", "").replaceFirst("\\`", "").replaceAll("[\\.\\{\\}\\[\\]\\'\\\"\\-\\`]", "_");
	}

	@Override
	public String findSortAlias(String columnDef) {
		return columnDef;
	}

	@Override
	public String makeSortDefinition(String column, Optional<String> customSortAlias) {
		return customSortAlias.orElseGet(() -> " MAX(" + column + ") AS " + makeSortAlias(column));
	}

	@Override
	public Identifier identify(String label) {
		return identify(label, true);
	}

	@Override
	public Identifier identify(String label, boolean quoted) {
		JdbcEnvironment jdbcEnvironment = getSessionMetadataIntegrator().getJdbcEnvironment();
		return getPhysicalNamingStrategy().toPhysicalColumnName(
				jdbcEnvironment.getIdentifierHelper().toIdentifier(label, quoted),
				jdbcEnvironment
		);
	}

	@Override
	public String getPhysicalTableName(HibFieldSchemaVersionElement<?,?,?,?,?> version) {
		return getPhysicalTableName(version, getPhysicalNamingStrategy(), getSessionMetadataIntegrator().getJdbcEnvironment(), getHibernateDialect());
	}

	@Override
	public String getPhysicalTableName(UUID schemaVersionUUID) {
		return getPhysicalTableNameIdentifier(schemaVersionUUID, getPhysicalNamingStrategy(), getSessionMetadataIntegrator().getJdbcEnvironment()).render(getHibernateDialect());
	}

	@Override
	public Identifier getPhysicalTableNameIdentifier(UUID versionUuid) {
		return getPhysicalTableNameIdentifier(versionUuid, getPhysicalNamingStrategy(), getSessionMetadataIntegrator().getJdbcEnvironment());
	}

	@Override
	public String getPhysicalTableName(HibFieldSchemaVersionElement<?,?,?,?,?> version, PhysicalNamingStrategy physicalTableNameStrategy, JdbcEnvironment jdbcEnvironment, Dialect dialect) {
		return getPhysicalTableNameIdentifier((UUID) version.getId(), physicalTableNameStrategy, jdbcEnvironment).render(dialect);
	}

	@Override
	public Identifier getPhysicalTableNameIdentifier(UUID versionUuid, PhysicalNamingStrategy physicalTableNameStrategy, JdbcEnvironment jdbcEnvironment) {
		return getPhysicalTableNameIdentifier(MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX + UUIDUtil.toShortUuid(versionUuid), physicalTableNameStrategy, getSessionMetadataIntegrator().getJdbcEnvironment());
	}

	@Override
	public String renderColumn(ContentColumn column) {
		if (column instanceof JoinedContentColumn) {
			return ((JoinedContentColumn) column).getColumnAlias();
		}
		return renderColumnUnsafe(column.getLabel(), false);
	}

	@Override
	public String renderColumnUnsafe(String column, boolean bypass) {
		return bypass ? column : identify(column).render(getHibernateDialect());
	}

	@Override
	public Optional<String> maybeGetDatabaseEntityName(Class<?> cls) {
		return Arrays.asList(cls.getAnnotationsByType(Entity.class)).stream().map(Entity::name).findAny();
	}

	@Override
	public Optional<Identifier> maybeGetPhysicalTableNameIdentifier(Class<?> cls, PhysicalNamingStrategy physicalTableNameStrategy, JdbcEnvironment jdbcEnvironment) {
		return maybeGetDatabaseEntityName(cls).map(id -> physicalTableNameStrategy.toPhysicalTableName(
				jdbcEnvironment.getIdentifierHelper().toIdentifier(id),
				jdbcEnvironment
		));
	}

	@Override
	public Optional<Identifier> maybeGetPhysicalTableNameIdentifier(Class<?> cls) {
		return maybeGetPhysicalTableNameIdentifier(cls, getPhysicalNamingStrategy(), getSessionMetadataIntegrator().getJdbcEnvironment());
	}

	@Override
	public Optional<String> maybeGetPhysicalTableName(Class<?> cls) {
		return maybeGetPhysicalTableNameIdentifier(cls).map(id -> id.render(getHibernateDialect()));
	}

	@Override
	public Optional<Set<String>> getDatabaseColumnNames(Class<?> cls) {
		return maybeGetPhysicalTableName(cls).map(tableName -> {
			try {
				ResultSet columnRs = HibernateUtil.getConnectionFromEntityManager(HibernateTx.get().entityManager())
						.getMetaData().getColumns(null, null, getSessionMetadataIntegrator().getJdbcEnvironment().getIdentifierHelper().toMetaDataObjectName(maybeGetPhysicalTableNameIdentifier(cls).get()), "%");
				Set<String> entityColumns = new HashSet<>();
				while (columnRs.next()) {
					String columnName = columnRs.getString("COLUMN_NAME");
					entityColumns.add(columnName);
				}
				return entityColumns;
			} catch (SQLException e1) {
				throw new IllegalStateException(e1);
			}	
		});
	}

	@Override
	public String getUUIDTypeName(Connection connection) {
		SessionMetadataIntegrator smi = getSessionMetadataIntegrator();
		TypeConfiguration typeConfig = smi.getSessionFactoryImplementor().getTypeConfiguration();
		int uuidType = smi.getBasicTypeForClass(UUID.class).getSqlTypeCodes(smi.getSessionFactoryImplementor())[0];
		return typeConfig.getDdlTypeRegistry().getTypeName(uuidType, new Size(null, null, (long) DEFAULT_UUID_LENGTH), typeConfig.getBasicTypeForJavaType(UUID.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <V, T extends HibListFieldEdge<V>> jakarta.persistence.Query makeListItemsMultiInsertionQuery(List<T> slice, EntityManager em, long startAt) {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ");
		sb.append(MeshTablePrefixStrategy.TABLE_NAME_PREFIX);
		sb.append(AbstractHibListFieldEdgeImpl.getEntityTableName((Class) slice.get(0).getClass()));
		sb.append(" (containertype, containeruuid, containerversionuuid, fieldkey, valueoruuid, itemindex, listuuid, dbuuid) VALUES ");
		sb.append(IntStream.range(0, slice.size()).mapToObj(index -> String.format(
				"(:containertype_%d, :containeruuid_%d, :containerversionuuid_%d, :fieldkey_%d, :valueoruuid_%d, :itemindex_%d, :listuuid_%d, :dbuuid_%d)", 
				index, index, index, index, index, index, index, index)).collect(Collectors.joining(",")));
		jakarta.persistence.Query query = em.createNativeQuery(sb.toString());
		IntStream.range(0, slice.size()).forEach(index -> {
			T item = slice.get(index);
			query.setParameter("containertype_" + index, item.getContainerType().name());
			query.setParameter("containeruuid_" + index, item.getContainerUuid());
			query.setParameter("containerversionuuid_" + index, item.getContainerVersionUuid());
			query.setParameter("fieldkey_" + index, item.getFieldName());
			query.setParameter("valueoruuid_" + index, item.getValueOrUuid());
			query.setParameter("itemindex_" + index, startAt + index);
			query.setParameter("listuuid_" + index, item.getListUuid());
			query.setParameter("dbuuid_" + index, item.getDbUuid());
		});
		return query;
	}

	@Override
	public long deleteContentEdgesByProject(EntityManager em, HibProject project) {
		return em.createQuery(DELETE_BY_ELEMENT_IN + "(" + FIND_BY_PROJECT + ")")
				.setParameter("project", project)
				.executeUpdate();
	}

	@Override
	public long deleteContentEdgesByUuids(EntityManager em, Collection<UUID> uuids) {
		return em.createQuery(DELETE_BY_ELEMENT_IN + "(" + FIND_BY_NODE_UUIDS + ")")
				.setParameter("nodesUuid", uuids)
				.executeUpdate();
	}

	@Override
	public long deleteContentEdgesByBranchUuids(EntityManager em, HibBranch branch, Collection<UUID> uuids) {
		return em.createQuery(DELETE_BY_ELEMENT_IN + "(" + FIND_BY_NODE_UUIDS_BRANCH + ")")
				.setParameter("nodesUuid", uuids)
				.setParameter("branch", branch)
				.executeUpdate();
	}

	@Override
	public String getUUIDTypeName() {
		EntityManager em = HibernateTx.get().entityManager();
		Connection connection = HibernateUtil.getConnectionFromEntityManager(em);
		return getUUIDTypeName(connection);
	}

	@Override
	public String getSqlTypeName(FieldTypes type, String uuidTypeName) {
		TypeConfiguration typeConfig = getSessionMetadataIntegrator().getSessionFactoryImplementor().getTypeConfiguration();
		Dialect dialect = getHibernateDialect();
		switch (type) {
			case STRING:
			case HTML:
				return typeConfig.getDdlTypeRegistry().getTypeName(
								Types.LONGVARCHAR, 
								new Size(getSqlTypePrecision(type), getSqlTypeScale(type), getSqlTypeLength(type)), 
								typeConfig.getBasicTypeForJavaType(String.class));
			case NUMBER:
				return typeConfig.getDdlTypeRegistry().getTypeName(
								Types.DOUBLE, 
								new Size(getSqlTypePrecision(type), getSqlTypeScale(type), getSqlTypeLength(type)), 
								typeConfig.getBasicTypeForJavaType(Number.class));
			case LIST:
			case MICRONODE:
			case NODE:
			case BINARY:
			case S3BINARY:
				return uuidTypeName;
			case BOOLEAN:
				return typeConfig.getDdlTypeRegistry().getTypeName(Types.BOOLEAN, dialect);
			case DATE:
				return typeConfig.getDdlTypeRegistry().getTypeName(Types.TIMESTAMP, dialect);
			default:
				throw new IllegalArgumentException("Unsupported field type: " + type);
		}
	}

	@Override
	public long getSqlTypeLength(FieldTypes type) {
		switch (type) {
		case NUMBER:
			return DEFAULT_NUMBER_LENGTH;
		case STRING:
		case HTML:
			return DEFAULT_STRING_LENGTH;
		case LIST:
		case MICRONODE:
		case NODE:
		case BINARY:
		case S3BINARY:
			return DEFAULT_UUID_LENGTH;
		default:
			return Size.DEFAULT_LENGTH;
		}
	}

	@Override
	public int getSqlTypePrecision(FieldTypes type) {
		switch (type) {
		case NUMBER:
			return DEFAULT_NUMBER_PRECISION;
		default:
			return Size.DEFAULT_PRECISION;
		}
	}

	@Override
	public int getSqlTypeScale(FieldTypes type) {
		switch (type) {
		case NUMBER:
			return DEFAULT_NUMBER_SCALE;
		default:
			return Size.DEFAULT_SCALE;
		}
	}

	@Override
	public boolean tableExists(Connection connection, UUID versionUuid) {
		try {
			DatabaseMetaData metaData = connection.getMetaData();
			Identifier physicalTableNameIdentifier = getPhysicalTableNameIdentifier(versionUuid);
			String metadataTable = getSessionMetadataIntegrator().getJdbcEnvironment().getIdentifierHelper().toMetaDataObjectName(physicalTableNameIdentifier);
			ResultSet rs = metaData.getTables(null, null, metadataTable, null);
			return rs.next();
		} catch (SQLException e) {
			return false;
		}
	}

	protected String getPagingOffsetParamName() {
		return "offset";
	}

	protected String getPagingLimitParamName() {
		return "limit";
	}

	protected <T> T instantiate(Class<T> cls, Supplier<T> instanceSupplier) {
		T instance;
		if ((instance = instanceSupplier.get()) == null) {
			synchronized (getClass()) {
				if ((instance = instanceSupplier.get()) == null) {
					try {
						return cls.getConstructor().newInstance();
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						log.fatal("Could not instantiate JDBC driver of class " + getJdbcDriverClass().getName());
						throw new IllegalArgumentException(e);
					}
				}
			}
		}
		return instance;
	}

	protected String getPhysicalTableNameIdentifier(String tableName) {
		return getPhysicalTableNameIdentifier(tableName, getPhysicalNamingStrategy(), getSessionMetadataIntegrator().getJdbcEnvironment()).render(getHibernateDialect());
	}

	protected Identifier getPhysicalTableNameIdentifier(String tableName, PhysicalNamingStrategy physicalTableNameStrategy, JdbcEnvironment jdbcEnvironment) {
		return physicalTableNameStrategy.toPhysicalTableName(
				jdbcEnvironment.getIdentifierHelper().toIdentifier(tableName),
				jdbcEnvironment
		);
	}
}
