package com.gentics.mesh.query;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.type.Type;

import com.gentics.mesh.hibernate.SessionMetadataIntegrator;
import com.gentics.mesh.hibernate.util.HibernateUtil;

/**
 * An interceptor implementation for the Hibernate session factory creation, allowing manipulation of JPA-unrelated container version tables, 
 * before JPA is ready to serve entity manipulation requests, that can potentially affect the versions and their tables.
 * 
 * @author plyhun
 *
 */
public class MetadataExtractorIntegrator implements SessionMetadataIntegrator {

	private Metadata metadata;
	private SessionFactoryImplementor sessionFactory;

	@Override
	public void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		this.metadata = metadata;
		this.sessionFactory = sessionFactory;
		dropContentColumnsIfRequired();
	}

	@Override
	public void disintegrate(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
	}

	public Dialect getDialect() {
		return sessionFactory.getJdbcServices().getDialect();
	}

	@Override
	public SessionFactoryImplementor getSessionFactoryImplementor() {
		return sessionFactory;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return sessionFactory.getJdbcServices().getJdbcEnvironment();
	}

	@Override
	public Type getBasicTypeForClass(Class<?> clazz) {
		return sessionFactory.getMetamodel().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType(clazz.getCanonicalName());
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public boolean isInitialized() {
		return metadata != null;
	}

	@Override
	public String getTableName(final Class<?> modelClazz) {
		EntityPersister persister = sessionFactory.getMetamodel().entityPersister(modelClazz);
		if (persister instanceof SingleTableEntityPersister) {
			return ((SingleTableEntityPersister) persister).getTableName();
		} else {
			throw new IllegalArgumentException(modelClazz + " does not map to a single table.");
		}
	}

	private void dropContentColumnsIfRequired() {
		if (HibernateUtil.shouldCleanDatabase(sessionFactory)) {
			// Here the main problem is that the sessionFactory provided is an incompletely constructed object.
			// This is actually violating the Java rules on the object construction, but here we have to live with it, sadly, and cannot fully use the sessionFactory.
			// Thus the session cannot be created, and we have to get to the JDBC connection.
			ConnectionProvider connectionProvider = sessionFactory.getSessionFactoryOptions().getServiceRegistry().getService(ConnectionProvider.class);
			try (Connection connection = connectionProvider.getConnection()) {
				HibernateUtil.dropContentTables(
						connection, 
						getDialect(), 
						getJdbcEnvironment(),
						getMetadata().getDatabase().getPhysicalNamingStrategy());
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
