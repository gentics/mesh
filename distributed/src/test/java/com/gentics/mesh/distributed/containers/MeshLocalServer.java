package com.gentics.mesh.distributed.containers;

import static com.gentics.mesh.Events.STARTUP_EVENT_ADDRESS;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;

public class MeshLocalServer extends TestWatcher {

	private static boolean inUse = false;
	/**
	 * Name of the node.
	 */
	private String nodeName;

	private MeshRestClient client;

	private boolean initCluster;

	private CountDownLatch waitingLatch = new CountDownLatch(1);

	private boolean waitForStartup;

	private int httpPort;
	
	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */

	public MeshLocalServer(String nodeName, boolean initCluster, boolean waitForStartup) {
		if (inUse) {
			throw new RuntimeException("The Mesh local server rule can't be used twice in the same JVM.");
		}
		inUse = true;
		this.initCluster = initCluster;
		this.nodeName = nodeName;
		this.waitForStartup = waitForStartup;
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
		options.setNodeName(nodeName);

		options.getStorageOptions().setDirectory(basePath + "/graph");
		options.getSearchOptions().setDirectory(basePath + "/es");
		options.getUploadOptions().setDirectory(basePath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(basePath + "/temp");
		options.getHttpServerOptions().setPort(httpPort);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getAuthenticationOptions().setKeystorePath(basePath + "/keystore.jkms");
		// options.getSearchOptions().setHttpEnabled(true);
		options.getClusterOptions().setEnabled(true);

		Mesh mesh = Mesh.mesh(options);
		mesh.getVertx().eventBus().consumer(STARTUP_EVENT_ADDRESS, mh -> {
			waitingLatch.countDown();
		});

		new Thread(() -> {
			try {
				mesh.run();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

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

	public MeshRestClient getMeshClient() {
		if (client == null) {
			client = MeshRestClient.create("localhost", httpPort, Mesh.vertx());
			client.setLogin("admin", "admin");
			client.login().toBlocking().value();
		}
		return client;
	}

}
