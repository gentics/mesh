package com.gentics.mesh.database;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourceUnit;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.ConfigUtils;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.hibernate.HazelcastLocalCacheRegionFactory;

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
			.put(AvailableSettings.JAKARTA_JDBC_URL, databaseConnector.getConnectionUrl())
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
				// Cache sizes are configured in com.gentics.mesh.database.cluster.HibClusterManager.getHazelcast()
				optionBuilder
						.put("hibernate.cache.region.factory_class", HazelcastLocalCacheRegionFactory.class.getName())
						.put("hibernate.cache.hazelcast.instance_name", options.getNodeName());
			} else {
				AtomicLong size = new AtomicLong();
				AtomicReference<ResourceUnit> unit = new AtomicReference<>();

				ConfigUtils.parseQuotaSetting(options.getCacheConfig().getNonClusteredHibernateCacheSize(),
						Runtime.getRuntime().maxMemory(), value -> {
							size.set(value);
							unit.set(MemoryUnit.B);
						}, value -> {
							size.set(value);
							unit.set(MemoryUnit.B);
						}, value -> {
							size.set(value);
							unit.set(EntryUnit.ENTRIES);
						}, value -> {
							size.set(50000);
							unit.set(EntryUnit.ENTRIES);
						}, () -> {
							size.set(50000);
							unit.set(EntryUnit.ENTRIES);
						});

				// create the cache configuration for the entity cache
				CacheConfiguration<Object, Object> entityCacheConfiguration = CacheConfigurationBuilder
						.newCacheConfigurationBuilder(Object.class, Object.class,
								ResourcePoolsBuilder.newResourcePoolsBuilder().heap(size.get(), unit.get()))
						.withExpiry(ExpiryPolicyBuilder.noExpiration()).build();

				// create the cache configuration for the other caches (query cache)
				CacheConfiguration<Object, Object> otherCacheConfiguration = CacheConfigurationBuilder
						.newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder.heap(10000))
						.withExpiry(ExpiryPolicyBuilder.noExpiration()).build();

				CachingProvider cachingProvider = Caching.getCachingProvider(EhcacheCachingProvider.class.getName());
				if (cachingProvider instanceof EhcacheCachingProvider ehCachingProvider) {
						CacheManager jCacheManager = ehCachingProvider.getCacheManager(ehCachingProvider.getDefaultURI(),
								ehCachingProvider.getDefaultClassLoader());
						jCacheManager.createCache("HibEntityCache",
								Eh107Configuration.fromEhcacheCacheConfiguration(entityCacheConfiguration));
						jCacheManager.createCache("default-update-timestamps-region",
								Eh107Configuration.fromEhcacheCacheConfiguration(otherCacheConfiguration));
						jCacheManager.createCache("default-query-results-region",
								Eh107Configuration.fromEhcacheCacheConfiguration(otherCacheConfiguration));
					}

				optionBuilder
						.put("hibernate.cache.region.factory_class", JCacheRegionFactory.class.getName())
						.put("hibernate.javax.cache.provider", EhcacheCachingProvider.class.getName());
			}
		} else {
			optionBuilder
				.put("hibernate.cache.use_query_cache", Boolean.FALSE.toString())
				.put("hibernate.cache.use_second_level_cache", Boolean.FALSE.toString());
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
