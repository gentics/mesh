package com.gentics.mesh.hibernate.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringEscapeUtils;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.slf4j.Logger;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.contentoperation.ContentNoCacheStorage;
import com.gentics.mesh.contentoperation.DynamicContentColumn;
import com.gentics.mesh.contentoperation.JoinedContentColumn;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.database.HibTxData;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.hibernate.HibernateStorageOptions;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.util.UUIDUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Utility functions for Hibernate-related components, DAOs and entities..
 */
public final class HibernateUtil {
	private static final Logger log = getLogger(HibernateUtil.class);

	public static final int DEFAULT_IN_QUERY_LIMIT = Short.MAX_VALUE;

	private HibernateUtil() {
	}

	/**
	 * Get a first result item of a query, or null.
	 *
	 * @param <T>
	 * @param query
	 * @return
	 */
	public static <T> T firstOrNull(TypedQuery<T> query) {
		List<T> resultList = query.setMaxResults(1).getResultList();
		return resultList.size() > 0 ? resultList.get(0) : null;
	}

	/**
	 * Get a first result item of a stream, or null.
	 *
	 * @param <T>
	 * @param query
	 * @return
	 */
	public static <T> T firstOrNull(Stream<? extends T> stream) {
		return stream.findAny().orElse(null);
	}

	/**
	 * Check if the database should be cleaned on server startup.
	 * 
	 * @param sessionFactory the session factory object with an access to the factory options.
	 * @return
	 */
	public static boolean shouldCleanDatabase(SessionFactoryImplementor sessionFactory) {
		SessionFactoryOptions settings = sessionFactory.getSessionFactoryOptions();
		return settings.getSchemaAutoTooling() == SchemaAutoTooling.CREATE_DROP
				|| settings.getSchemaAutoTooling() == SchemaAutoTooling.CREATE
				|| settings.getSchemaAutoTooling() == SchemaAutoTooling.DROP;
	}

	/**
	 * List the content (nodes/micronodes) tables existing currently in the database. Requires Hibernate to be around.
	 * 
	 * @param connection JDBC connection
	 * @param jdbcEnvironment JDBC environment
	 * @param physicalNamingStrategy table naming strategy
	 * @return
	 */
	public static Set<String> getContentTables(Connection connection, JdbcEnvironment jdbcEnvironment, PhysicalNamingStrategy physicalNamingStrategy) {
		String tableNamePattern = jdbcEnvironment.getIdentifierHelper().toMetaDataObjectName(
				physicalNamingStrategy.toPhysicalTableName(jdbcEnvironment.getIdentifierHelper()
							.toIdentifier(MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX), jdbcEnvironment));
		return getContentTables(connection, tableNamePattern);
	}

	/**
	 * List the content (nodes/micronodes) tables existing currently in the database. Required JDBC environment to be set up.
	 * 
	 * @param connection JDBC connection
	 * @param tableNamePattern content table name pattern
	 * @return
	 */
	public static Set<String> getContentTables(Connection connection, String tableNamePattern) {
		ResultSet rs = null;
		Set<String> tables = new HashSet<>();
		try {
			rs = connection.getMetaData().getColumns(null, connection.getSchema(), tableNamePattern + "%", null);
			int columnCount = rs.getMetaData().getColumnCount() + 1;
			while (rs.next()) {
				for (int i = 1; i < columnCount; i++) {
					try (Statement stmt = connection.createStatement()) {
						Object column = rs.getObject(i);
						if (column == null) {
							continue;
						}
						String columnValue = column.toString();
						if (columnValue.startsWith(tableNamePattern)) {
							tables.add(columnValue);
							break;
						}
					} catch (Throwable e) {
						log.error("Cannot drop the content table", e);
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					log.error("Cannot close result set", e);
				}
			}
		}
		return tables;
	}

	/**
	 * Drop content tables.<br>
	 * Since this method can be called from the Hibernate initialization stage, where there are neither EntityManager nor the Session available,
	 * a set of JDBC level params has to be provided.
	 *
	 * @param connection JDBC connection
	 * @param dialect Hibernate dialect of the underlying DB
	 * @param jdbcEnvironment JDBC environment
	 * @param physicalNamingStrategy 
	 */
	public static void dropContentTables(Connection connection, Dialect dialect, JdbcEnvironment jdbcEnvironment, PhysicalNamingStrategy physicalNamingStrategy) {
		Set<String> dropped = new HashSet<>();
		Set<String> contentTables = getContentTables(connection, jdbcEnvironment, physicalNamingStrategy);
		for (String tableName : contentTables) {
			try (Statement stmt = connection.createStatement()) {
				if (!dropped.contains(tableName)) {
					log.info("Deleting the table " + tableName);
					stmt.execute(dialect.getDropTableString(tableName));
					dropped.add(tableName);
					break;
				}
			} catch (Throwable e) {
				log.error("Cannot drop the content table", e);
			}
		}
	}

	/**
	 * Get the JDBC connection out of an Entity Manager.
	 *
	 * @param entityManager
	 * @return
	 */
	public static Connection getConnectionFromEntityManager(EntityManager entityManager) {
		return entityManager.unwrap(SharedSessionContractImplementor.class).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
	}

	/**
	 * Load all rows of the given Entity Manager's entity type.
	 *
	 * @param <B>
	 * @param em
	 * @param classOfT
	 * @return
	 */
	public static <B> Stream<B> loadAll(EntityManager em, Class<B> classOfT) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<B> cq = cb.createQuery(classOfT);
		Root<B> rootEntry = cq.from(classOfT);
		CriteriaQuery<B> all = cq.select(rootEntry);
		TypedQuery<B> allQuery = em.createQuery(all);
		return allQuery.getResultStream();
	}

	/**
	 * Make a map of column alias/content definitions for the given Schema version
	 * 
	 * @param version
	 * @return
	 */
	public static Map<String, ContentColumn> collectVersionColumnsByAlias(FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		return collectVersionColumnsByAlias(collectVersionColumns(version));
	}

	/**
	 * Make a map of column alias/content definitions for the given column definition list
	 * 
	 * @param version
	 * @return
	 */
	@Deprecated
	public static Map<String, ContentColumn> collectVersionColumnsByAlias(List<ContentColumn> columns) {
		return columns.stream().collect(Collectors.toMap(col -> HibernateTx.get().data().getDatabaseConnector().renderColumn(col), Function.identity()));
	}

	/**
	 * Make a list of ContentColumn definitions for the given Schema version.
	 * 
	 * @param version
	 * @return
	 */
	public static List<ContentColumn> collectVersionColumns(FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		Stream<ContentColumn> commonColumns = (version instanceof SchemaVersion) ?
				HibNodeFieldContainerImpl.COMMON_COLUMNS.stream() :
				HibMicronodeContainerImpl.COMMON_COLUMNS.stream();
	
		Stream<DynamicContentColumn> dynamicContentColumnStream = version.getSchema()
				.getFields()
				.stream()
				.map(DynamicContentColumn::new);

		// we will join the content to the binary and s3binary tables to get the fileNames, which are used for the
		// path calculation
		Stream<JoinedContentColumn> joinedContentColumns = version.getSchema().getFields().stream()
				.map(FieldSchema::getType)
				.filter(type -> FieldTypes.BINARY.toString().equals(type) || FieldTypes.S3BINARY.toString().equals(type))
				.distinct()
				.flatMap(type -> {
					if (FieldTypes.BINARY.toString().equals(type)) {
						return Stream.of(JoinedContentColumn.BINARY_FILENAME, JoinedContentColumn.BINARY_FIELDKEY);
					} else if (FieldTypes.S3BINARY.toString().equals(type)) {
						return Stream.of(JoinedContentColumn.S3_FILENAME, JoinedContentColumn.S3_FIELDKEY);
					} else {
						return Stream.empty();
					}
				});
		return Stream.concat(
				commonColumns,
				Stream.concat(dynamicContentColumnStream, joinedContentColumns))
				.collect(Collectors.toList());
	}

	/**
	 *  Make a SQL SELECT content column set clause.
	 * 
	 * @param columns
	 * @return
	 */
	public static String makeContentSelectClause(List<ContentColumn> columns) {
		return makeContentSelectClause(columns, Optional.empty(), false);
	}

	/**
	 * Make a SQL SELECT content column set clause.
	 * 
	 * @param columns
	 * @param maybeCustomAlias
	 * @param forceUnlob check if the columns should be checked for requiring unblobbing.
	 * @return
	 */
	public static String makeContentSelectClause(List<ContentColumn> columns, Optional<String> maybeCustomAlias, boolean forceUnlob) {
		return streamContentSelectClause(HibernateTx.get().data().getDatabaseConnector(), columns, maybeCustomAlias, forceUnlob).collect(Collectors.joining(","));
	}

	/**
	 * Make a SQL SELECT content column set clause, streamed for the collection of choice.
	 * 
	 * @param columns
	 * @param maybeCustomAlias
	 * @param forceUnlob check if the columns should be checked for requiring unblobbing.
	 * @return
	 */
	public static Stream<String> streamContentSelectClause(DatabaseConnector databaseConnector, List<ContentColumn> columns, Optional<String> maybeCustomAlias, boolean forceUnlob) {
		return columns.stream()
			.filter(c -> maybeCustomAlias.isEmpty() || !JoinedContentColumn.class.isInstance(c))
			.map(c -> {
				String renderedColumn = databaseConnector.renderColumn(c);
				if (c instanceof JoinedContentColumn) {
					return maybeCustomAlias.orElseGet(() -> ((JoinedContentColumn) c).getTableAlias()) + "." + ((JoinedContentColumn) c).getColumnName() + " as " + renderedColumn;
				}
				String columnName = maybeCustomAlias.orElse(ContentNoCacheStorage.DEFAULT_ALIAS) + "." + renderedColumn;
				if (forceUnlob) {
					columnName = databaseConnector.installStringContentColumn(columnName, true, false);
				}
				return columnName;
			});
	}

	/**
	 * Make SQL SELECT clause for a content version table.
	 * 
	 * @param version
	 * @param maybeCustomAlias apply custom table alias, if applicable.
	 * @return
	 */
	public static String makeContentSelectClause(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, Optional<String> maybeCustomAlias) {
		return makeContentSelectClause(collectVersionColumns(version), maybeCustomAlias, false);
	}

	/**
	 * Return the number of parameter limit for sql in queries
	 * @return
	 */
	public static int inQueriesLimit() {
		HibTxData data = HibernateTx.get().data();
		HibernateStorageOptions options = data.options().getStorageOptions();
		switch (options.getSqlParametersLimit()) {
		case HibernateStorageOptions.SQL_PARAMETERS_LIMIT_OPTION_DB_DEFINED:
			return data.getDatabaseConnector().getQueryParametersCountLimit();
		case HibernateStorageOptions.SQL_PARAMETERS_LIMIT_OPTION_UNLIMITED:
			return Integer.MAX_VALUE;
		default:
			int limit = Integer.parseInt(options.getSqlParametersLimit());
			return limit > 0 ? limit : DEFAULT_IN_QUERY_LIMIT;
		}
	}

	/**
	 * Return the number of parameter limit for sql in queries, including the splitting query params number, for usage in {@link SplittingUtils}.
	 * 
	 * @param availableParams splitting query params number. Usually this is a number of parameters, provided to a query along with the set of parameters to be split. 
	 * For instance, at {@link NodeDaoImpl#getFieldContainers} there are 3 stable parameters + a number of role parameters, so it is 3 + roles.size() to put here.
	 * 
	 * @return
	 */
	public static int inQueriesLimitForSplitting(int availableParams) {
		// 5 has been heuristically estimated as a bound value to cover all the (found so far) usecases.
		int limit = inQueriesLimit() - availableParams - 5;
		if (limit < 1) {
			limit = DEFAULT_IN_QUERY_LIMIT;
		}
		return limit;
	}

	/**
	 * Make a native query param name out of param value.
	 * 
	 * @param value
	 * @return
	 */
	public static String makeParamName(Object value) {
		return "p" + Integer.toHexString(value.hashCode());
	}

	/**
	 * Make a table/column alias for an arbitrary name. 
	 * 
	 * @param owner
	 * @return
	 */
	public static String makeAlias(String owner) {
		return StringEscapeUtils.escapeSql(owner.toLowerCase()) + "_";
	}

	/**
	 * Make a table alias for an element type.
	 * 
	 * @param owner
	 * @return
	 */
	public static String makeAlias(ElementType owner) {
		return makeAlias(owner.name());
	}

	/**
	 * Drop a many-to-many edge of role-group relation.
	 * 
	 * @param em
	 * @param role
	 * @param group
	 * @return
	 */
	public static int dropGroupRoleConnection(EntityManager em, Role role, Group group) {
		HibernateTx tx = HibernateTx.get();
		DatabaseConnector dc = tx.data().getDatabaseConnector();

		Object roleUuid = role.getId();
		Object groupUuid = group.getId();
		String roleUuidParam = makeParamName(roleUuid);
		String groupUuidParam = makeParamName(groupUuid);
		int ret = em.createNativeQuery("DELETE FROM " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "group_role WHERE " 
					+ dc.renderNonContentColumn("roles_dbUuid") + " = :" + roleUuidParam + " AND " 
					+ dc.renderNonContentColumn("groups_dbUuid") + " = :" + groupUuidParam)
			.setParameter(roleUuidParam, roleUuid).setParameter(groupUuidParam, groupUuid).executeUpdate();
		tx.refresh(group);
		tx.refresh(role);
		return ret;
	}

	/**
	 * Drop a many-to-many edge of user-group relation.
	 * 
	 * @param em
	 * @param user
	 * @param group
	 * @return
	 */
	public static int dropGroupUserConnection(EntityManager em, User user, Group group) {
		HibernateTx tx = HibernateTx.get();
		DatabaseConnector dc = tx.data().getDatabaseConnector();

		Object userUuid = user.getId();
		Object groupUuid = group.getId();
		String userUuidParam = makeParamName(userUuid);
		String groupUuidParam = makeParamName(groupUuid);
		int ret = em.createNativeQuery("DELETE FROM " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "group_user WHERE " 
					+ dc.renderNonContentColumn("users_dbUuid") + " = :" + userUuidParam + " AND " 
					+ dc.renderNonContentColumn("groups_dbUuid") + " = :" + groupUuidParam)
			.setParameter(userUuidParam, userUuid).setParameter(groupUuidParam, groupUuid).executeUpdate();
		tx.refresh(group);
		tx.refresh(user);
		return ret;
	}

	/**
	 * Silently convert a String UUID object to Java UUID, or return self.
	 * 
	 * @param uuid
	 * @return
	 */
	public static Object toJavaUuidOrSelf(Object uuid) {
		if (uuid != null && UUIDUtil.isUUID(uuid.toString())) {
			return UUIDUtil.toJavaUuid(uuid.toString());
		} else {
			return uuid;
		}
	}

	/**
	 * Drop a many-to-many edge of field-variant relation.
	 * 
	 * @param em
	 * @param variant
	 * @param field
	 * @return
	 */
	public static int dropGroupUserConnection(EntityManager em, ImageVariant variant, HibBinaryFieldEdgeImpl field) {
		HibernateTx tx = HibernateTx.get();
		DatabaseConnector dc = tx.data().getDatabaseConnector();

		Object variantUuid = variant.getId();
		Object fieldUuid = field.getId();
		String variantUuidParam = makeParamName(variantUuid);
		String fieldUuidParam = makeParamName(fieldUuid);
		int ret = em.createNativeQuery("DELETE FROM " + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "binary_field_variant WHERE " 
					+ dc.renderNonContentColumn("variants_dbUuid") + " = :" + variantUuidParam + " AND " 
					+ dc.renderNonContentColumn("fields_dbUuid") + " = :" + fieldUuidParam)
			.setParameter(variantUuidParam, variantUuid).setParameter(fieldUuidParam, fieldUuid).executeUpdate();
		tx.refresh(field);
		tx.refresh(variant);
		return ret;
	}
}
