package com.gentics.mesh.database;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.metric.SimpleMetric.TX;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.stat.HibernateMetrics;
import org.hibernate.stat.HibernateQueryMetrics;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.db.CommonDatabase;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ClusterServerConfig;
import com.gentics.mesh.core.rest.admin.cluster.ServerRole;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.dagger.tx.TransactionComponent;
import com.gentics.mesh.database.cluster.HibClusterManager;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.hibernate.HibernateStorageOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;
import com.gentics.mesh.liquibase.LiquibaseConnectionProvider;
import com.gentics.mesh.liquibase.LiquibaseLogService;
import com.gentics.mesh.liquibase.LiquibaseStartupContext;
import com.gentics.mesh.liquibase.LiquibaseUIService;
import com.gentics.mesh.metric.MetricsService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;

import dagger.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaQuery;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.Scope;

/**
 * Actual Hibernate-backed DB implementation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibernateDatabase extends CommonDatabase implements Database, Serializable {
	private static final long serialVersionUID = -5624750153838267051L;

	private final Lazy<Vertx> vertx;
	private final TransactionComponent.Factory txFactory;
	private final DatabaseProvider databaseProvider;
	private final HibernateMeshOptions options;
	private final WriteLock writeLock;
	private final HibClusterManager clusterManager;
	private final Counter txCounter;
	private final LiquibaseConnectionProvider liquibaseConnectionProvider;
	private final ContentCachedStorage cachedStorage;
	private final DatabaseConnector databaseConnector;

	private EntityManagerFactory factory;

	@Inject
	public HibernateDatabase(
			HibernateMeshOptions options, DatabaseConnector connector, Lazy<Vertx> vertx, Mesh mesh,
			TransactionComponent.Factory txFactory, WriteLock writeLock,
			MetricsService metrics, LiquibaseConnectionProvider liquibaseConnectionProvider, HibClusterManager clusterManager,
			ContentCachedStorage cachedStorage) {
		super(mesh, metrics);
		this.options = options;
		this.databaseConnector = connector;
		this.vertx = vertx;
		this.txFactory = txFactory;
		this.writeLock = writeLock;
		this.liquibaseConnectionProvider = liquibaseConnectionProvider;
		this.databaseProvider = new DefaultSQLDatabase(this.options, this.databaseConnector);
		this.clusterManager = clusterManager;
		this.txCounter = metrics.counter(TX);
		this.cachedStorage = cachedStorage;
	}

	@Override
	public void init(String meshVersion, String... basePaths) throws Exception {
		if (cachedStorage.isContentCachingEnabled()) {
			log.info("Loading content cache");
			cachedStorage.init(this, metrics.isEnabled());
		}
		log.info("Loading hibernate...");

		Map<String, Object> hibernateProperties = databaseProvider.init();

		factory = Persistence.createEntityManagerFactory("default", hibernateProperties);

		EntityManager em = factory.createEntityManager();
		Session session = (Session) em.getDelegate();
		session.doWork(connection -> {
			try {
				Scope.child(Map.of(Scope.Attr.ui.name(), new LiquibaseUIService(), Scope.Attr.logService.name(),
						new LiquibaseLogService()), () -> {
					try (LiquibaseStartupContext lbctx = liquibaseConnectionProvider.getLiquibase(connection)) {
						Liquibase liquibase = lbctx.liquibase();
						liquibase.update((Contexts) null);
					}
				});
			} catch (Exception e) {
				log.error("Database migration failed", e);
				throw new RuntimeException("Database migration failed", e);
			} finally {
				em.close();
			}
		});

		if (metrics.isEnabled()) {
			initializeMetrics();
		}
	}

	private void initializeMetrics() {
		ConnectionProvider provider = ((SessionFactoryImpl) getEntityManagerFactory()).getSessionFactory().getSessionFactoryOptions().getServiceRegistry().getService(ConnectionProvider.class);
		HikariDataSource dataSource = (HikariDataSource) provider.unwrap(DataSource.class);
		dataSource.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(metrics.getMetricRegistry()));
		SessionFactory sessionFactory = getEntityManagerFactory().unwrap(SessionFactory.class);
		String sessionFactoryName = "default";
		HibernateQueryMetrics.monitor(metrics.getMetricRegistry(), sessionFactory, sessionFactoryName);
		HibernateMetrics.monitor(metrics.getMetricRegistry(), sessionFactory, sessionFactoryName);

		if (options.getStorageOptions().isSecondLevelCacheEnabled()) {
			cachedStorage.registerMetrics(metrics);
		}
	}

	@Override
	public void stop() {
		while (Tx.get() != null) {
			Tx.get().close();
		}
		factory.close();
		databaseProvider.close();
	}

	@Override
	public void setupConnectionPool() throws Exception {
		// no explicit use
	}

	@Override
	public void closeConnectionPool() {
		// no explicit use
	}

	@Override
	public void clear() {
		try {
			log.info("Clearing the database...");

			EntityManager em = factory.createEntityManager();
			Session session = (Session) em.getDelegate();
			session.doWork(connection -> {
				try {
					Scope.child(Map.of(Scope.Attr.ui.name(), new LiquibaseUIService(), Scope.Attr.logService.name(),
							new LiquibaseLogService()), () -> {
						try (LiquibaseStartupContext lbctx = liquibaseConnectionProvider.getLiquibase(connection)) {
							Liquibase liquibase = lbctx.liquibase();
							liquibase.dropAll();
							liquibase.update((Contexts) null);
						}
					});
				} catch (Exception e) {
					log.error("Clearing database failed", e);
					throw new RuntimeException("Clearing database failed failed", e);
				} finally {
					em.close();
				}
			});

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void exportDatabase(String outputDirectory) throws IOException {
		throw new IllegalStateException("Not implemented. Please use export function of your DB vendor");
	}

	@Override
	public void importDatabase(String importFile) throws IOException {
		throw new IllegalStateException("Not implemented. Please use import function of your DB vendor");
	}

	@Override
	public String backupDatabase(String backupDirectory) throws IOException {
		throw new IllegalStateException("Not implemented. Please use backup function of your DB vendor");
	}

	@Override
	public void restoreDatabase(String backupFile) throws IOException {
		throw new IllegalStateException("Not implemented. Please use restore function of your DB vendor");
	}

	@Override
	public String getVendorName() {
		try (Tx tx = tx()) {
			Connection connection = getJdbcConnection(tx.unwrap());
			String dbName = connection.getMetaData().getDatabaseProductName();
			String driverVersion = connection.getMetaData().getDriverVersion();
			int jdbcMajor = connection.getMetaData().getJDBCMajorVersion();
			int jdbcMinor = connection.getMetaData().getJDBCMajorVersion();
			int dbMajor = connection.getMetaData().getDatabaseMajorVersion();
			int dbMinor = connection.getMetaData().getDatabaseMinorVersion();
			return String.format("%s v%d.%d / JDBC v%d.%d / driver version %s / %s", dbName, dbMajor, dbMinor, jdbcMajor, jdbcMinor, driverVersion, databaseProvider.getProviderDescription());
		} catch (SQLException e) {
			log.error("Could not retrieve the database versions", e);
		}
		return databaseProvider.getProviderDescription();
	}

	@Override
	public String getVersion() {
		try (Tx tx = tx()) {
			Connection connection = getJdbcConnection(tx.unwrap());
			return connection.getMetaData().getDatabaseProductVersion();
		} catch (SQLException e) {
			log.error("Could not retrieve the database versions", e);
		}
		return "Unknown";
	}

	@Override
	public void enableMassInsert() {
		// no use
	}

	@Override
	public void resetIntent() {
		// no use
	}

	@Override
	public void setMassInsertIntent() {
		// no use
	}

	@Override
	public void shutdown() {
		factory.close();
	}

	@Override
	public ClusterManager clusterManager() {
		return clusterManager;
	}

	@Override
	public List<String> getChangeUuidList() {
		return Collections.emptyList();
	}

	@Override
	public Vertx vertx() {
		return vertx.get();
	}

	@Override
	public void updateClusterConfig(ClusterConfigRequest request) {
		// nothing to do, in the hibernate context changing reading/writing quorum values or member roles
		// has no effect
		log.warn("update cluster configuration call was ignored");
	}

	@Override
	public ClusterConfigResponse loadClusterConfig() {
		if (options.getClusterOptions().isEnabled() && clusterManager != null) {
			ClusterConfigResponse response = new ClusterConfigResponse();

			clusterManager.getVertxClusterManager().getNodes().forEach(node -> {
				ClusterServerConfig serverConfig = new ClusterServerConfig();
				serverConfig.setName(node);
				serverConfig.setRole(ServerRole.MASTER); // all mesh nodes can write/read with hibernate

				response.getServers().add(serverConfig);
			});

			// relational databases don't depend on mesh for reading/writing
			response.setReadQuorum(1);
			response.setWriteQuorum(String.valueOf(1));

			return response;
		} else {
			throw error(BAD_REQUEST, "error_cluster_status_only_available_in_cluster_mode");
		}
	}

	@Override
	public void blockingTopologyLockCheck() {
		// nothing to do
	}

	@Override
	public void setToMaster() {
		log.warn("setToMaster call is ignored");
	}

	@Override
	public WriteLock writeLock() {
		return writeLock;
	}

	@Override
	public HibernateTx tx() {
		HibernateTx tx = txFactory.create().tx();
		if (metrics.isEnabled()) {
			txCounter.increment();
		}
		return tx;
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		T handlerResult = null;
		boolean handlerFinished = false;
		int maxRetry = options.getStorageOptions().getRetryLimit();
		Throwable cause = null;
		Optional<EventQueueBatch> maybeBatch = Optional.empty();
		for (int retry = 0; retry < maxRetry; retry++) {
			Timer.Sample sample = Timer.start();
			// Check the status to prevent transactions during shutdown
			checkStatus();
			HibernateWork<T> work = null;
			try (HibernateTx tx = tx()) {
				SessionImpl session = tx.<HibernateTx>unwrap().getSessionImpl();
				work = new HibernateWork<>(tx, txHandler);
				session.doWork(work);
				handlerResult = work.getResult();
				handlerFinished = true;
				tx.success();
				maybeBatch = tx.data().maybeGetEventQueueBatch();
			} catch (GenericRestException e) {
				// Don't log. Just throw it along so that others can handle it
				throw e;
			} catch (RuntimeException e) {
				if (retryAfterException(e)) {
					cause = e;
					if (log.isTraceEnabled()) {
						log.trace("Error while handling transaction. Retrying " + retry, e);
					}
					int delay = options.getStorageOptions().getRetryDelayMillis();
					if (retry > 0 && delay > 0) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					// Reset previous result
					handlerFinished = false;
					handlerResult = null;
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Error handling transaction", e);
					}
					throw e;
				}
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Error handling transaction", e);
				}
				throw new RuntimeException("Transaction error", e);
			} finally {
				sample.stop(txTimer);
			}
			if (!handlerFinished && log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
				if (metrics.isEnabled()) {
					txRetryCounter.increment();
				}
			}
			if (handlerFinished) {
				maybeBatch.ifPresent(EventQueueBatch::dispatch);
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit {" + maxRetry + "} for trx exceeded", cause);
	}

	/**
	 * Check whether the throw exception could be "resolved" by retrying.
	 * This will check, whether the exception is itself one of the following exceptions, or has one of them as cause:
	 * <ul>
	 * <li>OptimisticLockException</li>
	 * <li>StaleStateException</li>
	 * <li>SQLTransientException</li>
	 * <li>LockAcquisitionException</li>
	 * </ul>
	 * @param t exception
	 * @return true for retry, false to fail immediately
	 */
	protected boolean retryAfterException(Throwable t) {
		if (t instanceof OptimisticLockException || t instanceof StaleStateException
				|| t instanceof SQLTransientException || t instanceof LockAcquisitionException) {
			return true;
		}
		if (t.getCause() != null) {
			return retryAfterException(t.getCause());
		} else {
			return false;
		}
	}

	@Override
	public boolean isEmptyDatabase() {
		try {
			return count(HibUserImpl.class) < 1;
		} catch (IllegalArgumentException e) {
			if ("Error occurred validating the Criteria".equals(e.getMessage())) {
				return true;
			} else {
				throw e;
			}
		}
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return factory;
	}

	@Override
	public void reload(HibElement element) {
		HibernateTx.get().entityManager().refresh(element, LockModeType.PESSIMISTIC_READ);
	}

	@Override
	public long count(Class<? extends HibBaseElement> clazz) {
		return tx(tx -> {
			return tx.<CommonTx>unwrap().count(clazz);
		});
	}

	@Override
	public String getElementVersion(HibElement element) {
		return element.getElementVersion();
	}

	@Override
	public <T extends HibElement> Iterator<? extends T> getElementsForType(Class<T> domainClass) {
		return tx(tx -> {
			EntityManager em = tx.<HibernateTx>unwrap().entityManager();
			CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(domainClass);
			query.from(domainClass);
			return em.createQuery(query).getResultStream().iterator();
		});
	}

	/**
	 * Evict all hibernate second level caches and content cache
	 */
	public void evictAll() {
		if (options.getStorageOptions().isSecondLevelCacheEnabled()) {
			SessionFactory sessionFactory = getEntityManagerFactory().unwrap(SessionFactory.class);
			sessionFactory.getCache().evictAll();
			cachedStorage.evictAll();
		}
	}

	/**
	 * Create a raw JDBC connection, unrelated to the current transaction mechanism. Please remember it is {@link AutoCloseable} and required actions in this regard!
	 * 
	 * @return
	 * @throws Exception on any inconvenience
	 */
	public Connection noTx() throws Exception {
		HibernateStorageOptions storageOptions = options.getStorageOptions();
		Driver driver = databaseConnector.getJdbcDriver();
		java.util.Properties info = new java.util.Properties();
        if (StringUtils.isNotBlank(storageOptions.getConnectionUsername())) {
            info.put("user", storageOptions.getConnectionUsername());
        }
        if (StringUtils.isNotBlank(storageOptions.getConnectionPassword())) {
            info.put("password", storageOptions.getConnectionPassword());
        }
		return driver.connect(databaseConnector.getConnectionUrl(), info);
	}

	/**
	 * Storage options getter
	 * 
	 * @return
	 */
	public HibernateStorageOptions getStorageOptions() {
		return options.getStorageOptions();
	}

	/**
	 * Database connector getter
	 * 
	 * @return
	 */
	public DatabaseConnector getDatabaseConnector() {
		return databaseConnector;
	}

	private Connection getJdbcConnection(HibernateTx tx) {
		return tx.entityManager().unwrap(SharedSessionContractImplementor.class).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
	}
}
