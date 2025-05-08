package com.gentics.mesh.test.local;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.AWSTestMode;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshOptionChanger.NoOptionChanger;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.MeshTestServer;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.SSLTestMode;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.Tuple;

public class MeshLocalServer extends TestWatcher implements MeshTestServer {

	private static boolean inUse = false;

	/**
	 * Name of the node.
	 */
	private String nodeName = "localServer_" + System.currentTimeMillis();

	private MeshRestClient client;

	private boolean initCluster = false;

	private CountDownLatch waitingLatch = new CountDownLatch(1);

	private boolean waitForStartup;

	private int httpPort;

	private String clusterName = null;

	private boolean clustering = false;

	private boolean isInMemory = false;

	private List<Tuple<Class<? extends MeshPlugin>, String>> plugins = new ArrayList<>();

	private Mesh mesh;

	private MeshOptions meshOptions;

	private MeshTestContextProvider contextProvider;

	/**
	 * Create a new local server.
	 * 
	 * @param clusterName
	 * @param initCluster
	 */
	public MeshLocalServer(MeshTestContextProvider contextProvider) {
		if (inUse) {
			throw new RuntimeException("The MeshLocalServer rule can't be used twice in the same JVM.");
		}
		inUse = true;
		this.contextProvider = contextProvider;
		this.meshOptions = contextProvider.getOptions();
	}

	@Override
	protected void finished(Description description) {
		super.finished(description);
		contextProvider.getInstanceProvider().teardownStorage();
	}

	@Override
	protected void starting(Description description) {
		try {
			contextProvider.getInstanceProvider().initPhysicalStorage(testSetting);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		String basePath = "target/" + nodeName;
		prepareFolder(basePath);

		this.httpPort = com.gentics.mesh.test.util.TestUtils.getRandomPort();
		System.setProperty("mesh.confDirName", basePath + "/config");

		if (nodeName != null) {
			meshOptions.setNodeName(nodeName);
		}
		meshOptions.setInitCluster(initCluster);
		meshOptions.getUploadOptions().setDirectory(basePath + "/binaryFiles");
		meshOptions.getUploadOptions().setTempDirectory(basePath + "/temp");
		meshOptions.getHttpServerOptions().setPort(httpPort);
		meshOptions.getHttpServerOptions().setEnableCors(true);
		meshOptions.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		meshOptions.getAuthenticationOptions().setKeystorePath(basePath + "/keystore.jkms");
		meshOptions.getMonitoringOptions().setEnabled(false);
		meshOptions.setInitialAdminPassword("admin");
		meshOptions.setForceInitialAdminPasswordReset(false);
		meshOptions.getSearchOptions().setUrl(null);

		meshOptions.getClusterOptions().setEnabled(clustering);
		if (clusterName != null) {
			meshOptions.getClusterOptions().setClusterName(clusterName);
		}

		mesh = Mesh.create(meshOptions);

		if (waitForStartup) {
			mesh.rxRun().blockingAwait(200, TimeUnit.SECONDS);
		} else {
			new Thread(() -> {
				try {
					mesh.run(false);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
		}
		plugins.forEach(t -> {
			mesh.deployPlugin(t.v1(), t.v2()).blockingAwait(10, TimeUnit.SECONDS);
		});
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
			client = MeshRestClient.create("localhost", httpPort, false);
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
	 * Return the created mesh instance.
	 * 
	 * @return
	 */
	public Mesh getMesh() {
		return mesh;
	}

	/**
	 * Automatically deploy the given plugin once the server is ready.
	 * 
	 * @param clazz
	 * @param id
	 * @return Fluent API
	 */
	public MeshLocalServer withPlugin(Class<? extends MeshPlugin> clazz, String id) {
		plugins.add(Tuple.tuple(clazz, id));
		return this;
	}

	/**
	 * Set the initial mesh options to be used by the server.
	 * 
	 * @param meshOptions
	 * @return Fluent API
	 */
	public MeshLocalServer withOptions(MeshOptions meshOptions) {
		this.meshOptions = meshOptions;
		return this;
	}

	/**
	 * Return the mesh options of the server.
	 * 
	 * @return
	 */
	public MeshOptions getMeshOptions() {
		return meshOptions;
	}

	protected final MeshTestSetting testSetting = new MeshTestSetting() {
		
		@Override
		public Class<? extends Annotation> annotationType() {
			return null;
		}
		
		@Override
		public boolean useKeycloak() {
			return false;
		}
		
		@Override
		public TestSize testSize() {
			return TestSize.FULL;
		}
		
		@Override
		public boolean synchronizeWrites() {
			return false;
		}
		
		@Override
		public boolean startStorageServer() {
			return true;
		}
		
		@Override
		public boolean startServer() {
			return false;
		}
		
		@Override
		public SSLTestMode ssl() {
			return SSLTestMode.OFF;
		}
		
		@Override
		public boolean resetBetweenTests() {
			return false;
		}
		
		@Override
		public MeshCoreOptionChanger optionChanger() {
			return MeshCoreOptionChanger.NO_CHANGE;
		}
		
		@Override
		public boolean monitoring() {
			return false;
		}
		
		@Override
		public boolean inMemoryDB() {
			return isInMemory;
		}
		
		@Override
		public ElasticsearchTestMode elasticsearch() {
			return ElasticsearchTestMode.NONE;
		}
		
		@Override
		public Class<? extends MeshOptionChanger> customOptionChanger() {
			return NoOptionChanger.class;
		}
		
		@Override
		public boolean clusterMode() {
			return clustering;
		}

		@Override
		public String clusterName() {
			return null;
		}

		@Override
		public int clusterInstances() {
			return 0;
		}

		@Override
		public String[] nodeNames() {
			return new String[0];
		}

		@Override
		public AWSTestMode awsContainer() {
			return AWSTestMode.NONE;
		}

		@Override
		public boolean loginClients() {
			return true;
		}
	};
}
