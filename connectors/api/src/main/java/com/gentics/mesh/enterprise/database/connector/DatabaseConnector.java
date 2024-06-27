package com.gentics.mesh.enterprise.database.connector;

import java.sql.Driver;

import org.hibernate.dialect.Dialect;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;

/**
 * A database connector interface for Gentics Mesh.
 */
public interface DatabaseConnector {

	/**
	 * Environment/property variable name.
	 */
	static final String MESH_DATABASE_CONNECTOR_ENV_PROP = "MESH_DATABASE_CONNECTOR";

	/**
	 * Get JDBC driver of a database, if already settled.
	 * 
	 * @return
	 */
	Driver getJdbcDriver();

	/**
	 * Get Hibernate database dialect, if already settled.
	 * 
	 * @return
	 */
	Dialect getHibernateDialect();

	/**
	 * Get JDBC driver class.
	 * 
	 * @return
	 */
	Class<? extends Driver> getJdbcDriverClass();

	/**
	 * Get Hibernate dialect class.
	 * 
	 * @return
	 */
	Class<? extends Dialect> getHibernateDialectClass();

	/**
	 * Get human-friendly description of this connector.
	 * 
	 * @return
	 */
	String getConnectorDescription();

	/**
	 * Set options instance to the connector.
	 * 
	 * @param options
	 * @return fluent
	 */
	DatabaseConnector setOptions(EnterpriseMeshOptions options);
}
