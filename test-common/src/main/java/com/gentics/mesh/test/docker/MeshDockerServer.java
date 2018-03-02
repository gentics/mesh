package com.gentics.mesh.test.docker;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestServer;

import io.vertx.core.Vertx;

/**
 * Test container for a mesh instance which uses local class files. The image for the container will automatically be rebuild during each startup.
 * 
 * @param <SELF>
 */
public class MeshDockerServer extends GenericContainer<MeshDockerServer> implements MeshTestServer {

	private static final Logger log = LoggerFactory.getLogger(MeshDockerServer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	private Vertx vertx;

	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */
	private Runnable startupAction = () -> {
		client = MeshRestClient.create(getContainerIpAddress(), getMappedPort(8080), false, vertx);
		client.setLogin("admin", "admin");
		client.login().blockingGet();
	};

	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node. Default: dummy
	 */
	private String nodeName = "dummy";

	private boolean waitForStartup;

	private int waitTimeout = 200;

	private Integer debugPort;

	private String extraOpts;

	private boolean startEmbeddedES = false;

	private boolean useFilesystem = false;

	/**
	 * Create a new docker server
	 * 
	 * @param vertx
	 *            Vertx instances used to create the rest client
	 */
	public MeshDockerServer(String meshImage, Vertx vertx) {
		super(meshImage);
		this.vertx = vertx;
		setWaitStrategy(new NoWaitStrategy());
	}

	public MeshDockerServer(Vertx vertx) {
		String version = MeshRestClient.getPlainVersion();
		if (version.endsWith("-SNAPSHOT")) {
			throw new RuntimeException(
				"It is not possible to run snapshot docker versions. Please use a final version of Gentics Mesh or manually specify a version.");
		}
		this.setDockerImageName("gentics/mesh:" + version);
		this.vertx = vertx;
		setWaitStrategy(new NoWaitStrategy());
	}

	@Override
	protected void configure() {
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv("NODENAME", nodeName);
		String javaOpts = null;

		if (debugPort != null) {
			javaOpts = "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n ";
			exposedPorts.add(8000);
			setPortBindings(Arrays.asList("8000:8000"));
		}

		if (extraOpts != null) {
			if (javaOpts == null) {
				javaOpts = "";
			}
			javaOpts += extraOpts + " ";
		}

		if (javaOpts != null) {
			addEnv("JAVAOPTS", javaOpts);
		}

		if (startEmbeddedES) {
			exposedPorts.add(9200);
			exposedPorts.add(9300);
		} else {
			// Don't run the embedded ES
			addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_START_EMBEDDED_ENV, "false");
			addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "null");
		}

		if (!useFilesystem) {
			addEnv(GraphStorageOptions.MESH_GRAPH_DB_DIRECTORY_ENV, "null");
		}

		exposedPorts.add(8080);
		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
		setStartupAttempts(1);
	}

	@Override
	public void start() {
		super.start();
		if (waitForStartup) {
			try {
				awaitStartup(waitTimeout);
			} catch (InterruptedException e) {
				throw new ContainerLaunchException("Container did not not startup on-time", e);
			}
		}
	}

	/**
	 * Block until the startup message has been seen in the container log output.
	 * 
	 * @param timeoutInSeconds
	 * @throws InterruptedException
	 */
	public void awaitStartup(int timeoutInSeconds) throws InterruptedException {
		startupConsumer.await(timeoutInSeconds, SECONDS);
	}

	public MeshRestClient client() {
		return client;
	}

	@Override
	public String getHostname() {
		return getContainerIpAddress();
	}

	@Override
	public int getPort() {
		return getMappedPort(8080);
	}

	/**
	 * Expose the debug port to connect to.
	 * 
	 * @param debugPort
	 *            JNLP debug port. No debugging is enabled when set to null.
	 * @return Fluent API
	 */
	public MeshDockerServer withDebug(int debugPort) {
		this.debugPort = debugPort;
		return this;
	}

	/**
	 * Wait until the mesh instance is ready.
	 * 
	 * @return Fluent API
	 */
	public MeshDockerServer waitForStartup() {
		waitForStartup = true;
		return this;
	}

	/**
	 * Wait until the mesh instance is ready.
	 * 
	 * @param waitTimeout
	 * @return
	 */
	public MeshDockerServer waitForStartup(int waitTimeout) {
		this.waitForStartup = true;
		this.waitTimeout = waitTimeout;
		return this;
	}

	/**
	 * Use the provided JVM arguments.
	 * 
	 * @param opts
	 *            Additional JVM options }
	 * @return
	 */
	public MeshDockerServer withExtraOpts(String opts) {
		extraOpts = opts;
		return this;
	}

	/**
	 * Set the name of the node.
	 * 
	 * @param name
	 * @return
	 */
	public MeshDockerServer withNodeName(String name) {
		this.nodeName = name;
		return this;
	}

	/**
	 * Start the embedded ES.
	 * 
	 * @return
	 */
	public MeshDockerServer withES() {
		this.startEmbeddedES = true;
		return this;
	}

	/**
	 * Run the mesh server with file system persistation enabled.
	 * 
	 * @return
	 */
	public MeshDockerServer withFilesystem() {
		this.useFilesystem = true;
		return this;
	}

}
