package com.gentics.mesh.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.type.Type;

/**
 * An extension to the {@link Integrator}, giving access to the Hibernate session metadata.
 */
public interface SessionMetadataIntegrator extends Integrator {

	/**
	 * Given a class representing an hibernate entity, return the table name
	 * @param modelClazz
	 * @return
	 */
	String getTableName(Class<?> modelClazz);

	/**
	 * Is Hibernate session factory initialized
	 * 
	 * @return
	 */
	boolean isInitialized();

	/**
	 * Get Hibernate ORM model metadata.
	 * 
	 * @return
	 */
	Metadata getMetadata();

	/**
	 * Get Hibernate type for a Java class.
	 * 
	 * @param clazz
	 * @return
	 */
	Type getBasicTypeForClass(Class<?> clazz);

	/**
	 * Extract JDBC environment from the session factory.
	 * 
	 * @return
	 */
	JdbcEnvironment getJdbcEnvironment();

	/**
	 * Extract an implementor from the session factory.
	 * 
	 * @return
	 */
	SessionFactoryImplementor getSessionFactoryImplementor();
}
