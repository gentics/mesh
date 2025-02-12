package com.gentics.mesh.test.docker;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.liquibase.LiquibaseConnectionProvider;
import com.gentics.mesh.liquibase.LiquibaseLogService;
import com.gentics.mesh.liquibase.LiquibaseStartupContext;
import com.gentics.mesh.liquibase.LiquibaseUIService;
import com.github.dockerjava.api.command.InspectContainerResponse;

import liquibase.Contexts;
import liquibase.Scope;

/**
 * Abstract implementation of a DatabaseContainer, which will prepare databases (schemas, users) in advance, for faster test execution
 *
 * @param <T> self type
 */
public abstract class PreparingDatabaseContainer<T extends PreparingDatabaseContainer<T>> extends DatabaseContainer<T> {
	public final static int POOL_SIZE = 10;

	public final static int PREPARE_THREADS = 1;

	public final static int DB_NAME_LENGTH = 10;

	public final static String DB_NAME_PREFIX = "mesh_test_";

	public final static int WAITING_TIMEOUT_S = 10 * 60;

	/**
	 * Service, which is used to create databases (schemas, users)
	 */
	private ExecutorService prepareService = Executors.newFixedThreadPool(PREPARE_THREADS);

	/**
	 * Service, which is used to drop databases (schemas, users)
	 */
	private ExecutorService disposeService = Executors.newCachedThreadPool();

	/**
	 * Pool of available databases
	 */
	protected BlockingQueue<String> pool = new ArrayBlockingQueue<>(POOL_SIZE * 2);

	static {
		// set threadsafe scopemanager
		Scope.setScopeManager(new ThreadLocalScopeManager());
	}

	/**
	 * Get a random database name with the configured prefix and length
	 * @return random name
	 */
	public static String getRandomName() {
		Random rand = new Random();

		char[] charArray = new char[DB_NAME_LENGTH];
		for (int i = 0; i < DB_NAME_LENGTH; i++) {
			charArray[i] = (char) ('a' + (char) rand.nextInt(26));
		}

		return DB_NAME_PREFIX + new String(charArray);
	}

	/**
	 * Create an instance using the given container
	 * @param container container name
	 */
	public PreparingDatabaseContainer(String container) {
		super(container);
	}

	@Override
	public void stop() {
		prepareService.shutdownNow();
		disposeService.shutdownNow();
		try {
			prepareService.awaitTermination(10, TimeUnit.SECONDS);
			disposeService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("Error while waiting for prepare service to shut down", e);
		}
		super.stop();
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		log.info("Preparing databases");
		for (int i = 0; i < POOL_SIZE; i++) {
			prepareDatabase(1, 3, 5);
		}
	}

	/**
	 * Prepare a databases. Pick a random name, create the database and let
	 * liquibase create the required tables. Finally, add the name to the pool.
	 * If backoff times are given and setup fails, the setup will be retried after waiting the given number of seconds (in order they are given).
	 * This will help for containers, which are not yet ready to be used (although the log messages indicated that).
	 * @param backOffSeconds optional array of backoff times (in seconds)
	 */
	protected void prepareDatabase(int...backOffSeconds) {
		prepareService.execute(() -> {
			int failedTries = 0;
			boolean retry = true;
			while (retry) {
				retry = false;
				String name = getRandomName();
				try {
					try (Connection c = getConnection()) {
						createDatabase(c, name);
					}
					try {
						Scope.child(Map.of(Scope.Attr.ui.name(), new LiquibaseUIService(), Scope.Attr.logService.name(),
								new LiquibaseLogService()), () -> {
									LiquibaseConnectionProvider liquibaseConnectionProvider = new LiquibaseConnectionProvider(null, null);
									try (Connection c = getConnection(name); LiquibaseStartupContext ctx = liquibaseConnectionProvider.getLiquibase(c)) {
										ctx.liquibase().update((Contexts) null);
									}
								});
					} catch (SQLException e) {
						throw e;
					} catch (Exception e) {
						throw new SQLException(e);
					}
					pool.add(name);
					log.info(String.format("Created %s (pool contains %d databases)", name, pool.size()));
				} catch (SQLException e) {
					failedTries++;
					if (backOffSeconds.length > 0 && failedTries <= backOffSeconds.length) {
						int wait = backOffSeconds[failedTries - 1];
						retry = true;
						log.warn(String.format("Creating database %s failed, retrying in %d seconds", name, wait), e);
						try {
							Thread.sleep(wait * 1000L);
						} catch (InterruptedException e1) {
						}
					} else {
						log.error("Error while setting up database", e);
					}
				}
			}
		});
	}

	/**
	 * Take the next available database from the pool (wait if one becomes available).
	 * Also start preparing a new database (asynchronously)
	 * @return name of the database
	 */
	public String take() {
		prepareDatabase();
		String name;
		try {
			name = pool.poll(WAITING_TIMEOUT_S, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if (name == null) {
			throw new RuntimeException(String.format("No database was available after waiting %d seconds", WAITING_TIMEOUT_S));
		}
		log.info("Taken {} (pool contains {} databases)", name, pool.size());
		return name;
	}

	/**
	 * Asynchronously dispose of the database with given name
	 * @param name name
	 */
	public void dispose(String name) {
		disposeService.execute(() -> {
			try {
				try (Connection c = getConnection()) {
					dropDatabase(c, name);
				}
				log.info("Database {} disposed", name);
			} catch (SQLException e) {
				log.error("Error while disposing of database " + name, e);
			}
		});
	}

	/**
	 * Get "general" connection to the db (used e.g. for creating/dropping databases)
	 * @return connection
	 * @throws SQLException
	 */
	protected abstract Connection getConnection() throws SQLException;

	/**
	 * Get a connection to the db manager for the given database (used for setting up tables)
	 * @param name database name
	 * @return connection
	 * @throws SQLException
	 */
	protected abstract Connection getConnection(String name) throws SQLException;

	/**
	 * Create a database with given name
	 * @param c connection
	 * @param name name
	 * @throws SQLException
	 */
	protected abstract void createDatabase(Connection c, String name) throws SQLException;

	/**
	 * Drop the database with given name
	 * @param c connection
	 * @param name name
	 * @throws SQLException
	 */
	protected abstract void dropDatabase(Connection c, String name) throws SQLException;
}
