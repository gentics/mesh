package com.gentics.mesh.database;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.hibernate.HazelcastLocalCacheRegionFactory;

import net.sf.ehcache.hibernate.EhCacheRegionFactory;

/**
 * Hibernate-backed DB wrapper.
 */
public class DefaultSQLDatabase implements DatabaseProvider {
	
	private static final String CHARSET = StandardCharsets.UTF_8.name();
	private final HibernateMeshOptions options;
	private final DatabaseConnector databaseConnector;
	
	public DefaultSQLDatabase(HibernateMeshOptions options, DatabaseConnector databaseConnector) {
		this.options = options;
		this.databaseConnector = databaseConnector;
	}

	@Override
	public Map<String, Object> init() {
		ImmutableMap.Builder<String, Object> optionsBuilder = ImmutableMap.builder();
		setConnectionOptions(optionsBuilder);
		setHikariOptions(optionsBuilder);
		setSecondLevelCacheOptions(optionsBuilder);
		setStatisticsOptions(optionsBuilder);
		setOtherOptions(optionsBuilder);

		return optionsBuilder.build();
	}

	@Override
	public String getProviderDescription() {
		return databaseConnector.getConnectorDescription();
	}

	private void setConnectionOptions(ImmutableMap.Builder<String, Object> optionBuilder) {
		optionBuilder
			.put(AvailableSettings.CONNECTION_PROVIDER, HikariCPConnectionProvider.class.getName())
			.put(AvailableSettings.JAKARTA_JDBC_DRIVER, databaseConnector.getJdbcDriverClass().getName())
			.put(AvailableSettings.JAKARTA_JDBC_URL, options.getStorageOptions().getConnectionUrl() 
					+ options.getStorageOptions().getDatabaseName() + options.getStorageOptions().getConnectionUrlExtraParams())
			.put(AvailableSettings.JAKARTA_JDBC_USER, options.getStorageOptions().getConnectionUsername())
			.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, options.getStorageOptions().getConnectionPassword())
			.put("hibernate.connection.useUnicode", "true")
			.put("hibernate.connection.characterEncoding", CHARSET)
			.put("hibernate.connection.charSet", CHARSET);
	}

	private void setHikariOptions(ImmutableMap.Builder<String, Object> optionBuilder) {
		optionBuilder
			.put("hibernate.hikari.autoCommit", Boolean.toString(options.getStorageOptions().getHikariOptions().isAutocommit()))
			.put("hibernate.hikari.connectionTimeout", String.valueOf(options.getStorageOptions().getHikariOptions().getConnectionTimeout()))
			.put("hibernate.hikari.idleTimeout", String.valueOf(options.getStorageOptions().getHikariOptions().getIdleTimeout()))
			.put("hibernate.hikari.maxLifetime", String.valueOf(options.getStorageOptions().getHikariOptions().getMaxLifetime()))
			.put("hibernate.hikari.minimumIdle", String.valueOf(options.getStorageOptions().getHikariOptions().getMinimumIdleConnection()))
			.put("hibernate.hikari.maximumPoolSize", String.valueOf(options.getStorageOptions().getHikariOptions().getMaxPoolSize()))
			.put("hibernate.hikari.poolName", options.getStorageOptions().getHikariOptions().getPoolName())
			.put("hibernate.hikari.registerMbeans", Boolean.toString(options.getStorageOptions().getHikariOptions().getRegisterMBeans()));

		if (options.getStorageOptions().getHikariOptions().getTransactionIsolationLevel() != null) {
			optionBuilder.put("hibernate.hikari.transactionIsolation", String.valueOf(options.getStorageOptions().getHikariOptions().getTransactionIsolationLevel()));
		}
		optionBuilder.put("hibernate.hikari.leakDetectionThreshold", String.valueOf(options.getStorageOptions().getHikariOptions().getLeakDetectionThreshold()));
	}

	private void setSecondLevelCacheOptions(ImmutableMap.Builder<String, Object> optionBuilder) {
		if (options.getStorageOptions().isSecondLevelCacheEnabled()) {
			optionBuilder
					.put("hibernate.cache.default_cache_concurrency_strategy", CacheConcurrencyStrategy.READ_WRITE.toAccessType().getExternalName())
					.put("hibernate.cache.use_query_cache", Boolean.TRUE.toString())
					.put("hibernate.cache.use_second_level_cache", Boolean.TRUE.toString());
			if (options.getClusterOptions().isEnabled()) {
				optionBuilder
						.put("hibernate.cache.region.factory_class", HazelcastLocalCacheRegionFactory.class.getName())
						.put("hibernate.cache.hazelcast.instance_name", options.getClusterOptions().getClusterName());
			} else {
				optionBuilder
						.put("hibernate.cache.region.factory_class", EhCacheRegionFactory.class.getName());
			}
		}
	}

	private void setStatisticsOptions(ImmutableMap.Builder<String, Object> optionBuilder) {
		optionBuilder.put(AvailableSettings.GENERATE_STATISTICS, options.getStorageOptions().isGenerateStatistics());
		optionBuilder.put(AvailableSettings.LOG_SLOW_QUERY, Long.toString(options.getStorageOptions().getSlowSqlThreshold()));
	}

	private void setOtherOptions(ImmutableMap.Builder<String, Object> optionBuilder) {
		optionBuilder
			.put(AvailableSettings.DIALECT, databaseConnector.getHibernateDialectClass().getName())
			// setup of the DB tables is done with liquibase
			.put(AvailableSettings.HBM2DDL_AUTO, "none")
			.put(AvailableSettings.HBM2DDL_CHARSET_NAME, CHARSET)
			.put(AvailableSettings.SHOW_SQL, Boolean.toString(options.getStorageOptions().isShowSql()))
			.put(AvailableSettings.FORMAT_SQL, Boolean.toString(options.getStorageOptions().isFormatSql()))
			.put(AvailableSettings.STATEMENT_BATCH_SIZE, String.valueOf(options.getStorageOptions().getJdbcBatchSize()))
			.put(AvailableSettings.ORDER_INSERTS, "true")
			.put(AvailableSettings.ORDER_UPDATES, "true")
			.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, databaseConnector.getPhysicalNamingStrategyClass().getCanonicalName())
			.put(AvailableSettings.INTERCEPTOR, ContentInterceptor.class.getName())
			.put(JpaSettings.INTEGRATOR_PROVIDER, (IntegratorProvider) () -> Collections.singletonList(databaseConnector.getSessionMetadataIntegrator()))
			// don't save timezones in the dates for backwards compatibility
			.put(AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE, "TIMESTAMP");
		if (options.getStorageOptions().getQueryTimeout() != 0) {
			optionBuilder.put("jakarta.persistence.query.timeout", String.valueOf(options.getStorageOptions().getQueryTimeout()));
		}
	}
}
