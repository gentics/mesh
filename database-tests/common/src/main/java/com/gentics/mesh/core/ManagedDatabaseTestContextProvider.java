package com.gentics.mesh.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshProviderOrder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
* Abstract base class for context providers, which use databases from the mesh-testdb-manager
*/
@MeshProviderOrder(1)
public abstract class ManagedDatabaseTestContextProvider extends HibernateTestContextProvider {
	/**
	 * Name of the environment variable specifying the host of the testdb manager
	 */
	public final static String MANAGER_HOST_ENV = "MESH_TESTDB_MANAGER_HOST";

	/**
	 * Name of the system property specifying the host of the testdb manager
	 */
	public final static String MANAGER_HOST_PROP = "mesh.testdb.manager.host";

	/**
	 * Name of the environment variable specifying the port of the testdb manager
	 */
	public final static String MANAGER_PORT_ENV = "MESH_TESTDB_MANAGER_PORT";

	/**
	 * Name of the system property specifying the port of the testdb manager
	 */
	public final static String MANAGER_PORT_PROP = "mesh.testdb.manager.port";

	/**
	 * Timeout in s for waiting for a prepared test DB
	 */
	public final static int DB_WAIT_TIMEOUT_S = 60;

	/**
	 * Check whether the test context provider is eligible, which is the case, when system environment variables
	 * MESH_TESTDB_MANAGER_HOST and MESH_TESTDB_MANAGER_PORT have been set.
	 * @return true if the test context provider class is eligible
	 */
	public static boolean isEligible() {
		return !StringUtils.isBlank(getHost()) && !StringUtils.isBlank(getPort());
	}

	/**
	 * Get the configured test db manager host (null if not configured)
	 * @return
	 */
	public static String getHost() {
		return System.getProperty(MANAGER_HOST_PROP, System.getenv(MANAGER_HOST_ENV));
	}

	/**
	 * Get the configured test db manager port (null if not configured)
	 * @return
	 */
	public static String getPort() {
		return System.getProperty(MANAGER_PORT_PROP, System.getenv(MANAGER_PORT_ENV));
	}

	/**
	 * Client used for the websocket connection
	 */
	protected OkHttpClient client;

	/**
	 * Websocket connection
	 */
	protected WebSocket webSocket;

	/**
	 * Object mapper for parsing JSON messages from the manager
	 */
	protected ObjectMapper mapper = new ObjectMapper();

	/**
	 * Test DB Settings (sent from the testdb manager)
	 */
	protected TestDBSettings dbSettings;

	/**
	 * Mesh Options
	 */
	protected HibernateMeshOptions meshOptions;

	/**
	 * Latch for the inbound message with the test DB settings
	 */
	protected CountDownLatch databaseMessageLatch;

	/**
	 * Create an instance.
	 * Connect to the testdb manager via WEbSocket
	 */
	public ManagedDatabaseTestContextProvider() {
		ManagedBy managedBy = Objects.requireNonNull(getClass().getAnnotation(ManagedBy.class),
				String.format("%s must be annotated with %s", getClass(), ManagedBy.class));
		try {
			client = new OkHttpClient.Builder()
					.build();
			Request request = new Request.Builder().url("ws://" + getHost() + ":"
					+ getPort() + "/ws/" + managedBy.name()).build();

			databaseMessageLatch = new CountDownLatch(1);
			webSocket = client.newWebSocket(request, new WebSocketListener() {
				@Override
				public void onMessage(WebSocket ws, String text) {
					try {
						dbSettings = mapper.readValue(text, TestDBSettings.class);
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
					if (databaseMessageLatch != null) {
						databaseMessageLatch.countDown();
					}
				}
			});

			assertThat(databaseMessageLatch.await(DB_WAIT_TIMEOUT_S, TimeUnit.SECONDS))
					.as(String.format("Database was available within %d seconds", DB_WAIT_TIMEOUT_S)).isTrue();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void teardownStorage() {
		super.teardownStorage();
		webSocket.close(1000, null);
	}

	@Override
	public boolean fastStorageCleanup(List<Database> dbs) throws Exception {
		databaseMessageLatch = new CountDownLatch(1);
		webSocket.send("reset");
		assertThat(databaseMessageLatch.await(DB_WAIT_TIMEOUT_S, TimeUnit.SECONDS))
				.as(String.format("Database was available within %d seconds", DB_WAIT_TIMEOUT_S)).isTrue();
		fillMeshOptions(meshOptions);
		for (Database db : dbs) {
			db.reset();
		}
		return true;
	}

	@Override
	public void fillMeshOptions(HibernateMeshOptions meshOptions) {
		this.meshOptions = meshOptions;
		meshOptions.getStorageOptions().setDatabaseAddress(dbSettings.getHost() + ":" + dbSettings.getPort());
		meshOptions.getStorageOptions().setConnectionUrlExtraParams(getConnectionUrlExtraParams());
		meshOptions.getStorageOptions().setDatabaseName(dbSettings.getDatabase());
		meshOptions.getStorageOptions().setConnectionUsername(getConnectionUsername());
		meshOptions.getStorageOptions().setConnectionPassword(dbSettings.getPassword());
	}

	/**
	 * Get the extra URL parameters for the test DB
	 * @return extra URL parameters
	 */
	protected String getConnectionUrlExtraParams() {
		return "";
	}

	/**
	 * Get the connection username
	 * @return connection username
	 */
	protected String getConnectionUsername() {
		return dbSettings.getUsername();
	}
}
