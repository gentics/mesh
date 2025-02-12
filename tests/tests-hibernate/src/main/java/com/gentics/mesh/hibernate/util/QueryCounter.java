package com.gentics.mesh.hibernate.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * {@link AutoCloseable} implementation which will count the number of queries executed by Hibernate between construction of the instance and {@link #close()}.
 * This will only work when hibernate is configured to generate statistics. This can be achieved by setting the {@link MeshTestSetting#customOptionChanger()} to the class {@link EnableHibernateStatistics}.
 */
public class QueryCounter implements AutoCloseable {

	/**
	 * Start count (filled in the constructor)
	 */
	protected @UnknownKeyFor @NonNull @Initialized long startCount = 0;

	/**
	 * Builder used for building the instance
	 */
	protected Builder builder;

	/**
	 * Create an instance from the given builder
	 * @param builder builder
	 */
	protected QueryCounter(Builder builder) {
		this.builder = builder;
		if (this.builder.clear) {
			clearStatistics();
		}
		startCount = getQueryExecutionCount();
	}

	@Override
	public void close() {
		long count = getCountSinceStart();

		if (builder.maxCount >= 0) {
			assertThat(count).as("Allowed number of executed queries").isLessThanOrEqualTo(builder.maxCount);
		}
	}

	/**
	 * Get the number of queries which were executed between the start and now
	 * @return number of queries
	 */
	public long getCountSinceStart() {
		return getQueryExecutionCount() - startCount;
	}

	/**
	 * Get the current query execution count from the mbean
	 * @return
	 */
	protected @UnknownKeyFor @NonNull @Initialized long getQueryExecutionCount() {
		try {
			return builder.databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor().getStatistics().getQueryExecutionCount();
		} catch (Exception e) {
			fail("Unable to get query count", e);
			return -1;
		}
	}

	/**
	 * Get the queries
	 * @return queries as newline separated string
	 */
	public String getQueries() {
		try {
			String[] queries = builder.databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor().getStatistics().getQueries();
			return Stream.of(queries).collect(Collectors.joining("\n"));
		} catch (Exception e) {
			fail("Unable to get queries", e);
			return null;
		}
	}

	/**
	 * Let the mbean clear its statistics
	 */
	protected void clearStatistics() {
		try {
			builder.databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor().getStatistics().clear();
		} catch (Exception e) {
			fail("Unable to clear statistics", e);
		}
	}

	/**
	 * Builder for instances of {@link QueryCounter}
	 */
	public static class Builder {
		/**
		 * Whether to clear the statistics
		 */
		protected boolean clear;

		/**
		 * Maximum allowed count
		 */
		protected int maxCount = -1;

		/**
		 * Database connector
		 */
		protected DatabaseConnector databaseConnector;

		/**
		 * Get a new instance of the {@link Builder}
		 * @return Builder instance
		 */
		public static Builder get() {
			return new Builder();
		}

		/**
		 * Assert that no more than the given number of queries are executed
		 * @param maxCount allowed number of queries
		 * @return fluent API
		 */
		public Builder assertNotMoreThan(int maxCount) {
			this.maxCount = maxCount;
			return this;
		}

		/**
		 * Set the database connector.
		 * 
		 * @param databaseConnector
		 * @return
		 */
		public Builder withDatabaseConnector(DatabaseConnector databaseConnector) {
			this.databaseConnector = databaseConnector;
			return this;
		}

		/**
		 * Clear the statistics before counting
		 * @return fluent API
		 */
		public Builder clear() {
			this.clear = true;
			return this;
		}

		/**
		 * Build the instance of {@link QueryCounter}
		 * @return instance
		 */
		public QueryCounter build() {
			return new QueryCounter(this);
		}
	}

	/**
	 * Implementation of {@link MeshOptionChanger} which will let hibernate expose its data via JMX and generate statistics.
	 * Also the migration trigger interval is set to 0, so that no running background job might execute additional queries
	 */
	public static class EnableHibernateStatistics implements MeshOptionChanger {
		@Override
		public void change(MeshOptions options) {
			HibernateMeshOptions hibernateOptions = (HibernateMeshOptions) options;
			hibernateOptions.getStorageOptions().setGenerateStatistics(true);
			options.setMigrationTriggerInterval(0);
		}
	}
}
