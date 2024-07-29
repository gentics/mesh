package com.gentics.mesh.database.connector;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.query.Query;

import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.node.field.HibListFieldEdge;
import com.gentics.mesh.parameter.PagingParameters;

import jakarta.persistence.EntityManager;

/**
 * Various DBMS dependent SQL query fixes.
 */
public interface QueryUtils {

	static final int DEFAULT_STRING_LENGTH = 1000000;
	static final int DEFAULT_UUID_LENGTH = 16;
	static final int DEFAULT_NUMBER_LENGTH = 64;
	static final int DEFAULT_NUMBER_PRECISION = 40;
	static final int DEFAULT_NUMBER_SCALE = DEFAULT_NUMBER_LENGTH - DEFAULT_NUMBER_PRECISION;

	/**
	 * Install paging fetch into the query.
	 * 
	 * @param query
	 * @param alias
	 * @param pagingInfo 
	 * @return
	 */
	String installPaging(String query, String alias, PagingParameters pagingInfo);

	/**
	 * Install count fetch into the query.
	 * 
	 * @param query
	 * @return
	 */
	String installCount(String query);

	/**
	 * Install paging parameters, if applicable.
	 * 
	 * @param <Q>
	 * @param query
	 * @param pagingInfo
	 * @return
	 */
	<T, Q extends Query<T>> Q installPagingArguments(Q query, PagingParameters pagingInfo);

	/**
	 * Install content column of a List derived type.
	 * 
	 * @param listItemType list item type, lower case (e.g. "number" or "string")
	 * @param listAlias list table alias
	 * @param operator comparison operator, any case
	 * @param paramName column parameter name
	 * 
	 * @return
	 */
	String installListContentColumn(String listItemType, String listAlias, String operator, String paramName);

	/**
	 * Install content column of a String-derived type (currently String and HTML).
	 * 
	 * @param columnName
	 * @param makeAlias
	 * @param customAlias
	 * @return
	 */
	String installStringContentColumn(String columnName, boolean makeAlias, boolean customAlias);

	/**
	 * Make a column alias for sorting purposes.
	 * 
	 * @return
	 */
	String makeSortAlias(String column);

	/**
	 * Find an alias in the column definition.
	 * 
	 * @param columnDef
	 * @return
	 */
	String findSortAlias(String columnDef);

	/**
	 * Make a sorting part of a SQL query.
	 * 
	 * @param column
	 * @param customSortAlias
	 * @return
	 */
	String makeSortDefinition(String column, Optional<String> customSortAlias);

	/**
	 * Get the maximum number of parameters the query of this DBMS can hold.
	 * 
	 * @return
	 */
	int getQueryParametersCountLimit();

	/**
	 * Render column name according to the RDBMS' and Hibernate naming conventions. Beware of using this on arbitrary strings!
	 * 
	 * @param column
	 * @param bypass bypass the render
	 * @return
	 */
	String renderColumnUnsafe(String column, boolean bypass);

	/**
	 * Render the content column name according to the RDBMS' and Hibernate naming conventions
	 * 
	 * @param column
	 * @return
	 */
	String renderColumn(ContentColumn column);

	/**
	 * Get the Hibernate identifier of the table as per SQL DB engine dialect.
	 * @return
	 */
	Identifier getPhysicalTableNameIdentifier(UUID versionUuid, PhysicalNamingStrategy physicalTableNameStrategy,
			JdbcEnvironment jdbcEnvironment);

	/**
	 * Get the name of the table as per SQL DB engine dialect.
	 * @return
	 */
	String getPhysicalTableName(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version,
			PhysicalNamingStrategy physicalTableNameStrategy, JdbcEnvironment jdbcEnvironment, Dialect dialect);

	/**
	 * Content table have the following naming scheme "mesh_content__${schemaVersionUUID}
	 * @param versionUuid
	 * @return
	 */
	Identifier getPhysicalTableNameIdentifier(UUID versionUuid);

	/**
	 * Content table have the following naming scheme "mesh_content__${schemaVersionUUID}
	 * @param schemaVersionUUID
	 * @return
	 */
	String getPhysicalTableName(UUID schemaVersionUUID);

	/**
	 * return the name of the table
	 * @param version
	 * @return
	 */
	String getPhysicalTableName(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version);

	/**
	 * Create an identifier for the given label
	 * @param label label
	 * @param quoted  Is the identifier to be quoted explicitly
	 * @return identifier
	 */
	Identifier identify(String label, boolean quoted);

	/**
	 * Create a quoted identifier for the given label
	 * @param label label
	 * @return identifier
	 */
	Identifier identify(String label);

	/**
	 * Render a non-content (non-derived from {@link HibUnmanagedFieldContainer} column name via current JDBC driver.
	 * 
	 * @param column
	 * @return
	 */
	String renderNonContentColumn(String column);

	/**
	 * Render the physical name of the table as per SQL DB engine dialect, if the given class is an entity.
	 * @return
	 */
	Optional<String> maybeGetPhysicalTableName(Class<?> cls);

	/**
	 * Get the Hibernate identifier of the table as per SQL DB engine dialect, if the given class is an entity.
	 * @return
	 */
	Optional<Identifier> maybeGetPhysicalTableNameIdentifier(Class<?> cls);

	/**
	 * Get the Hibernate identifier of the table as per SQL DB engine dialect if the given class is an entity.
	 * @return
	 */
	Optional<Identifier> maybeGetPhysicalTableNameIdentifier(Class<?> cls, PhysicalNamingStrategy physicalTableNameStrategy, JdbcEnvironment jdbcEnvironment);

	/**
	 * Get the database entity name for a database entity class, if managed by Hibernate.
	 * 
	 * @param cls
	 * @return
	 */
	Optional<String> maybeGetDatabaseEntityName(Class<?> cls);

	/**
	 * Get actual column names for a Hibernate entity class.
	 * 
	 * @param cls
	 * @return
	 */
	Optional<Set<String>> getDatabaseColumnNames(Class<?> cls);

	/**
	 * Get the name of the dialect-dependent type for UUID.
	 *
	 * @param connection
	 * @return
	 */
	String getUUIDTypeName(Connection connection);

	/**
	 * Get a dummy comparison SQL predicate.
	 * 
	 * @param params query params holder
	 * @param mustPass should the dummy succeed?
	 * @return
	 */
	String getDummyComparison(Map<String, Object> params, boolean mustPass);

	/**
	 * Create a mass insertion query for the list field items.
	 * 
	 * @param <V> list item value type
	 * @param <T> list item type
	 * @param items items to insert 
	 * @param em entity manager
	 * @param startAt offset
	 * @return
	 */
	<V, T extends HibListFieldEdge<V>> jakarta.persistence.Query makeListItemsMultiInsertionQuery(List<T> items, EntityManager em, long startAt);

	/**
	 * Delete all content edges of a project.
	 * 
	 * @param project
	 * @param em
	 * @return
	 */
	long deleteContentEdgesByProject(EntityManager em, HibProject project);

	/**
	 * Delete selected content edges by UUIDs.
	 * 
	 * @param uuids
	 * @param em
	 * @return
	 */
	long deleteContentEdgesByUuids(EntityManager em, Collection<UUID> uuids);

	/**
	 * Delete selected content edges by UUIDs, that belong to the specific branch.
	 * 
	 * @param uuids
	 * @param em
	 * @param branch
	 * @return
	 */
	long deleteContentEdgesByBranchUuids(EntityManager em, HibBranch branch, Collection<UUID> uuids);

	/**
	 * Check if the JPA-unmanaged table exists for the (micro)schema container version.
	 *
	 * @param connection
	 * @param versionUuid
	 * @return
	 */
	boolean tableExists(Connection connection, UUID versionUuid);

	/**
	 * Get Hibernate SQL type size scale for the content field type.
	 * 
	 * @param type
	 * @return
	 */
	int getSqlTypeScale(FieldTypes type);

	/**
	 * Get Hibernate SQL type precision for the content field type.
	 * 
	 * @param type
	 * @return
	 */
	int getSqlTypePrecision(FieldTypes type);

	/**
	 * Get Hibernate SQL type length for the content field type.
	 * 
	 * @param type
	 * @return
	 */
	long getSqlTypeLength(FieldTypes type);

	/**
	 * Find a corresponding SQL type name for a column {@link FieldTypes} unstance.
	 * 
	 * @param type
	 * @param uuidTypeName a special type for UUIDs
	 * @return
	 */
	String getSqlTypeName(FieldTypes type, String uuidTypeName);

	/**
	 * Get the name of the dialect-dependent type for UUID.
	 *
	 * @return
	 */
	String getUUIDTypeName();
}
