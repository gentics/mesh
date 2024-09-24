package com.gentics.mesh.database.connector;

import java.sql.Driver;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.dialect.Dialect;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.SessionMetadataIntegrator;

/**
 * A database connector interface for Gentics Mesh.
 */
public interface DatabaseConnector extends QueryUtils {

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
	 * Get Hibernate session factory metadata instance.
	 * 
	 * @return
	 */
	SessionMetadataIntegrator getSessionMetadataIntegrator();

	/**
	 * Get Hibernate object physical namming strategy.
	 * 
	 * @return
	 */
	PhysicalNamingStrategy getPhysicalNamingStrategy();

	/**
	 * Get {@link PhysicalNamingStrategy} implementor class.
	 * 
	 * @return
	 */
	Class<? extends PhysicalNamingStrategy> getPhysicalNamingStrategyClass();

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
	DatabaseConnector setOptions(HibernateMeshOptions options);

	/**
	 * Get the JDBC full connection URL
	 * 
	 * @return
	 */
	String getConnectionUrl();
}
