package com.gentics.mesh.enterprise.database.connector;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.query.Query;

import com.gentics.mesh.contentoperation.ContentColumn;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Various DBMS dependent SQL query fixes.
 */
public interface QueryUtils {

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
}
