package com.gentics.mesh.enterprise.database.connector;

import static io.vertx.core.logging.LoggerFactory.getLogger;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.util.function.Supplier;

import org.hibernate.dialect.Dialect;

import com.gentics.mesh.query.MetadataExtractorIntegrator;

import io.vertx.core.logging.Logger;

/**
 * Base functionality for the Mesh Database Connector
 * 
 * @param <DR>
 * @param <DI>
 */
public abstract class AbstractDatabaseConnector<DR extends Driver, DI extends Dialect> implements DatabaseConnector {

	private static final Logger log = getLogger(AbstractDatabaseConnector.class);

	protected static Driver DRIVER = null;

	// Force no-arg ctor for the reflected instantiation
	protected AbstractDatabaseConnector() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public DR getJdbcDriver() {
		// TODO Get the driver from Hibernate, if possible
		return instantiate((Class<DR>) getJdbcDriverClass(), () -> (DR) DRIVER);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DI getHibernateDialect() {
		return MetadataExtractorIntegrator.INSTANCE.isInitialized() ? (DI) MetadataExtractorIntegrator.INSTANCE.getDialect() : instantiate((Class<DI>) getHibernateDialectClass(), () -> getHibernateDialect());
	}

	@Override
	public String getConnectorDescription() {
		return getJdbcDriverClass() + " / " + getHibernateDialectClass();
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
}
