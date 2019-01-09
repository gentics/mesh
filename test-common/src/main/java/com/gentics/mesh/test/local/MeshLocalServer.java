package com.gentics.mesh.test.local;

import static com.gentics.mesh.Events.STARTUP_EVENT_ADDRESS;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestServer;

public class MeshLocalServer extends TestWatcher implements MeshTestServer {

	private static boolean inUse = false;
	/**
	 * Name of the node.
	 */
	private String nodeName;

	private MeshRestClient client;

	private boolean initCluster = false;

	private CountDownLatch waitingLatch = new CountDownLatch(1);

	private boolean waitForStartup;

	private int httpPort;

	private String clusterName = null;

	private boolean clustering = false;

	private boolean startEmbeddedES = false;

	private boolean isInMemory = false;

	private Mesh mesh;

	/**
	 * Create a new local server.
	 * 
	 * @param clusterName
	 * @param initCluster
	 */
	public MeshLocalServer() {
		if (inUse) {
			throw new RuntimeException("The MeshLocalServer rule can't be used twice in the same JVM.");
		}
		inUse = true;
	}

	@Override
	protected void starting(Description description) {
		String basePath = "target/" + nodeName;
		prepareFolder(basePath);

		this.httpPort = com.gentics.mesh.test.util.TestUtils.getRandomPort();
		System.setProperty("mesh.confDirName", basePath + "/config");

		String[] args = new String[] {};
		if (initCluster) {
			args = new String[] { "-" + MeshCLI.INIT_CLUSTER };
		}
		MeshOptions options = OptionsLoader.createOrloadOptions(args);
		if (nodeName != null) {
			options.setNodeName(nodeName);
		}
		if (isInMemory) {
			options.getStorageOptions().setDirectory(null);
		} else {
			options.getStorageOptions().setDirectory(basePath + "/graph");
		}
		options.getUploadOptions().setDirectory(basePath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(basePath + "/temp");
		options.getHttpServerOptions().setPort(httpPort);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getAuthenticationOptions().setKeystorePath(basePath + "/keystore.jkms");
		options.getSearchOptions().setStartEmbedded(startEmbeddedES);
		if (!startEmbeddedES) {
			options.getSearchOptions().setUrl(null);
		}

		options.getClusterOptions().setEnabled(clustering);
		if (clusterName != null) {
			options.getClusterOptions().setClusterName(clusterName);
		}

		mesh = Mesh.mesh(options);

		new Thread(() -> {
			try {
				mesh.run();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

		while (mesh.getVertx() == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		mesh.getVertx().eventBus().consumer(STARTUP_EVENT_ADDRESS, mh -> {
			waitingLatch.countDown();
		});

		if (waitForStartup) {
			try {
				awaitStartup(200);
			} catch (InterruptedException e) {
				throw new RuntimeException("Local mesh instance did not not startup on-time", e);
			}
		}

	}

	@Override
	protected void finalize() throws Throwable {
		inUse = false;
	}

	/**
	 * Ensures that an empty folder exists for the given path.
	 * 
	 * @param path
	 * @throws IOException
	 */
	private static void prepareFolder(String path) {
		try {
			File folder = new File(path);
			FileUtils.deleteDirectory(folder);
			folder.mkdirs();
		} catch (Exception e) {
			throw new RuntimeException("Error while preparing folder for path {" + path + "}", e);
		}
	}

	/**
	 * Block until the startup message has been seen in the container log output.
	 * 
	 * @param timeoutInSeconds
	 * @throws InterruptedException
	 */
	public void awaitStartup(int timeoutInSeconds) throws InterruptedException {
		waitingLatch.await(timeoutInSeconds, TimeUnit.SECONDS);
	}

	@Override
	public MeshRestClient client() {
		if (client == null) {
			client = MeshRestClient.create("localhost", httpPort, false, Mesh.vertx());
			client.setLogin("admin", "admin");
			client.login().blockingGet();
		}
		return client;
	}

	@Override
	public String getHostname() {
		return "localhost";
	}

	@Override
	public int getPort() {
		return httpPort;
	}

	/**
	 * Set the name of the node.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public MeshLocalServer withNodeName(String name) {
		this.nodeName = name;
		return this;
	}

	/**
	 * Wait until the mesh instance is ready.
	 * 
	 * @return Fluent API
	 */
	public MeshLocalServer waitForStartup() {
		waitForStartup = true;
		return this;
	}

	/**
	 * Set the name of the cluster.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public MeshLocalServer withClusterName(String name) {
		this.clusterName = name;
		return this;
	}

	/**
	 * Set the init cluster flag.
	 * 
	 * @return
	 */
	public MeshLocalServer withInitCluster() {
		this.initCluster = true;
		return this;
	}

	/**
	 * Set the init cluster flag.
	 * 
	 * @return Fluent API
	 */
	public MeshLocalServer withClustering() {
		this.clustering = true;
		return this;
	}

	/**
	 * Set the memory mode flag.
	 * 
	 * @return Fluent API
	 */
	public MeshLocalServer withInMemoryMode() {
		this.isInMemory = true;
		return this;
	}

	/**
	 * Start the embedded ES
	 * 
	 * @return Fluent API
	 */
	public MeshLocalServer withES() {
		this.startEmbeddedES = true;
		return this;
	}

	/**
	 * Return the created mesh instance.
	 * 
	 * @return
	 */
	public Mesh getMesh() {
		return mesh;
	}

}
