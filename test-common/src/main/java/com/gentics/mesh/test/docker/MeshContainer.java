package com.gentics.mesh.test.docker;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.TestEnvironment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.util.UnixUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

import io.vertx.core.json.JsonObject;

/**
 * Test container for a mesh instance which uses local class files. The image for the container will automatically be rebuild during each startup.
 * 
 * @param <SELF>
 */
public class MeshContainer extends GenericContainer<MeshContainer> {

	private static final String PATH_UPLOADS = "/uploads";

	private static final String PATH_BACKUP = "/backup";

	private static final String PATH_GRAPHDB = "/graphdb";

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final Logger log = LoggerFactory.getLogger(MeshContainer.class);

	private static ImageFromDockerfile cachedImage = null;

	/**
	 * Local provider for docker image. The provider will utilize the class and jar files from a local Gentics Mesh checkout. This way a development version of
	 * Gentics Mesh can be used in a container test setup.
	 */
	public static final Supplier<ImageFromDockerfile> LOCAL_PROVIDER = () -> {
		if (cachedImage == null) {
			cachedImage = prepareDockerImage(true);
		}
		return cachedImage;
	};

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */
	private Runnable startupAction = () -> {
		client = MeshRestClient.create(getContainerIpAddress(), getMappedPort(8080), false);
		login();
	};

	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node. Default: dummy
	 */
	private String nodeName = "dummy";

	/**
	 * -1 = "majority"
	 */
	private int writeQuorum = -1;

	private boolean initCluster = false;

	private boolean waitForStartup = false;

	private boolean clearDataFolders = false;

	private Integer debugPort;

	private String clusterName;

	private String extraOpts;

	private String dataPathPostfix;

	private boolean startEmbeddedES = false;

	private CoordinatorMode coordinatorMode = null;

	private String coordinatorPlaneRegex;

	private boolean useFilesystem = false;
	
	private Map<String, ContainerPath> pathOverrides = new HashMap<>(3);
	
	public MeshContainer(Supplier<ImageFromDockerfile> imageProvider) {
		init();
		setImage(imageProvider.get());
		setWaitStrategy(new NoWaitStrategy());
	}

	public MeshContainer(String imageName) {
		init();
		this.setDockerImageName(imageName);
		setWaitStrategy(new NoWaitStrategy());
	}
	
	protected void init() {
		pathOverrides.put(PATH_GRAPHDB, new ContainerPath(StringUtils.EMPTY, BindMode.READ_WRITE));
		pathOverrides.put(PATH_BACKUP, new ContainerPath(StringUtils.EMPTY, BindMode.READ_WRITE));
		pathOverrides.put(PATH_UPLOADS, new ContainerPath(StringUtils.EMPTY, BindMode.READ_WRITE));
	}

	@Override
	protected void configure() {
		String instanceFolderName = isClustered() ? clusterName + "-" + nodeName : nodeName;
		File containerFolder = new File("target", "mesh-containers");
		File baseFolder = new File(containerFolder, instanceFolderName);
		String basePath = baseFolder.getAbsolutePath();
		log.info("Using base folder {}", basePath);
		String confPath = basePath + "/config";
		if (useFilesystem) {
			ContainerPath graphDbPath = pathOverrides.get(PATH_GRAPHDB);
			ContainerPath backupPath = pathOverrides.get(PATH_BACKUP);
			ContainerPath uploadsPath = pathOverrides.get(PATH_UPLOADS);
			
			String dataGraphDBPath = StringUtils.isNotBlank(graphDbPath.hostPath) ? graphDbPath.hostPath : (basePath + "/data-graphdb-" + dataPathPostfix);
			String dataBackupPath = StringUtils.isNotBlank(backupPath.hostPath) ? backupPath.hostPath : (basePath + "/data-backup-" + dataPathPostfix);			
			String dataUploadsPath = StringUtils.isNotBlank(uploadsPath.hostPath) ? uploadsPath.hostPath : (basePath + "/data-uploads-" + dataPathPostfix);

			// Ensure that the folder is created upfront. This is important to keep the uid and gids correct.
			// Otherwise the folder would be created by docker using root.

			if (clearDataFolders) {
				try {
					prepareFolder(dataGraphDBPath);
				} catch (Exception e) {
					fail("Could not setup bind folder {" + dataGraphDBPath + "}");
				}
				try {
					prepareFolder(dataBackupPath);
				} catch (Exception e) {
					fail("Could not setup bind folder {" + dataBackupPath + "}");
				}
				try {
					prepareFolder(dataUploadsPath);
				} catch (Exception e) {
					fail("Could not setup bind folder {" + dataUploadsPath + "}");
				}
			}
			addFileSystemBind(dataGraphDBPath, PATH_GRAPHDB, graphDbPath.bindMode);
			addFileSystemBind(dataBackupPath, PATH_BACKUP, backupPath.bindMode);
			addFileSystemBind(dataUploadsPath, PATH_UPLOADS, uploadsPath.bindMode);
			// withCreateContainerCmdModifier(it -> it.withVolumes(new Volume("/data")));
		}

		if (isClustered()) {
			try {
				new File(confPath).mkdirs();
				File confFile = new File(confPath, "default-distributed-db-config.json");
				String jsonConfig = generateDistributedConfig(writeQuorum).encodePrettily();
				FileUtils.writeStringToFile(confFile, jsonConfig, Charset.forName("UTF-8"));
				addFileSystemBind(confFile.getAbsolutePath(), "/config/default-distributed-db-config.json", BindMode.READ_ONLY);
			} catch (Exception e) {
				throw new RuntimeException("Error while creating default-distributed-db-config.json", e);
			}
		}

		changeUserInContainer();
		if (initCluster) {
			addEnv(MeshOptions.MESH_CLUSTER_INIT_ENV, "true");
		}
		List<Integer> exposedPorts = new ArrayList<>();
		if (nodeName != null) {
			addEnv(MeshOptions.MESH_NODE_NAME_ENV, nodeName);
		}
		if (clusterName != null) {
			addEnv(ClusterOptions.MESH_CLUSTER_NAME_ENV, clusterName);
			addEnv(ClusterOptions.MESH_CLUSTER_ENABLED_ENV, "true");
		}
		addEnv(ClusterOptions.MESH_CLUSTER_VERTX_PORT_ENV, "8123");
		addEnv(MeshOptions.MESH_PLUGIN_DIR_ENV, "/plugins");

		if (startEmbeddedES) {
			exposedPorts.add(9200);
			exposedPorts.add(9300);
		} else {
			// Don't run the embedded ES
			addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_START_EMBEDDED_ENV, "false");
			addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "null");
		}

		addEnv(MeshOptions.MESH_INITIAL_ADMIN_PASSWORD_ENV, "admin");
		addEnv(MeshOptions.MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET_ENV, "false");

		if (!useFilesystem) {
			addEnv(GraphStorageOptions.MESH_GRAPH_DB_DIRECTORY_ENV, "null");
		} else {
			addEnv(GraphStorageOptions.MESH_GRAPH_DB_DIRECTORY_ENV, "/graphdb");
		}

		if (coordinatorMode != null) {
			addEnv(ClusterOptions.MESH_CLUSTER_COORDINATOR_MODE_ENV, coordinatorMode.name());
		}

		if (coordinatorPlaneRegex != null) {
			addEnv(ClusterOptions.MESH_CLUSTER_COORDINATOR_REGEX_ENV, coordinatorPlaneRegex);
		}

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

		exposedPorts.add(8600);
		exposedPorts.add(8080);

		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
		setStartupAttempts(1);
	}

	private void changeUserInContainer() {
		int uid = 1000;
		try {
			uid = UnixUtils.getUid();
		} catch (IOException e) {
			e.printStackTrace();
		}
		final int id = uid;
		withCreateContainerCmdModifier(it -> it.withUser(id + ":" + id));
	}

	@Override
	public void start() {
		try {
			super.start();
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
		if (waitForStartup) {
			try {
				awaitStartup(500);
			} catch (InterruptedException e) {
				throw new ContainerLaunchException("Container did not not startup on-time", e);
			}
		}
	}

	@Override
	public void stop() {
		log.info("Stopping node {" + getNodeName() + "} of cluster {" + getClusterName() + "} Id: {" + getContainerId() + "}");
		dockerClient.stopContainerCmd(getContainerId()).exec();
		super.stop();
	}

	public void killContainer() {
		dockerClient.killContainerCmd(getContainerId()).withSignal("SIGTERM").exec();
		super.stop();
	}

	public void killHardContainer() {
		dockerClient.killContainerCmd(getContainerId()).withSignal("SIGKILL").exec();
		super.stop();
	}

	@Override
	public void close() {
		stop();
	}

	/**
	 * Ensures that an empty folder exists for the given path.
	 * 
	 * @param path
	 * @throws IOException
	 */
	private static void prepareFolder(String path) throws IOException {
		File folder = new File(path);
		FileUtils.deleteDirectory(folder);
		folder.mkdirs();
	}

	/**
	 * Prepare the docker image for the container which will contain all locally found classes.
	 * 
	 * @param enableClustering
	 * @return
	 */
	public static ImageFromDockerfile prepareDockerImage(boolean enableClustering) {
		ImageFromDockerfile dockerImage = new ImageFromDockerfile("mesh-local", true);
		try {
			File projectRoot = new File("..");

			// Locate all class folders
			List<Path> classFolders = Files.walk(projectRoot.toPath())
				.filter(file -> "classes".equals(file.toFile().getName()))
				.filter(file -> !file.toFile().getAbsolutePath().contains("test/plugins"))
				.collect(Collectors.toList());

			// Iterate over all classes in the class folders and add those to the docker context
			String classPathArg = "";
			for (Path path : classFolders) {
				// Prepare the class path argument
				classPathArg += ":" + path.toFile().getPath().replaceAll("\\.\\.\\/", "bin/");
				List<Path> classPaths = Files.walk(path).collect(Collectors.toList());
				for (Path classPath : classPaths) {
					if (classPath.toFile().isFile()) {
						File classFile = classPath.toFile();
						assertTrue("Could not find class file {" + classFile + "}", classFile.exists());
						String filePath = classPath.toFile().getPath();
						String dockerPath = filePath.replaceAll("\\.\\.\\/", "bin/");
						dockerImage.withFileFromFile(dockerPath, classFile);
					}
				}
			}
			classPathArg = classPathArg.substring(1);

			// Add maven libs
			File libFolder = new File("../server/target/mavendependencies-sharedlibs");
			assertTrue("The library folder {" + libFolder + "} could not be found", libFolder.exists());
			for (File lib : libFolder.listFiles()) {
				String dockerPath = lib.getPath().replaceAll("\\.\\.\\/", "bin/");
				classPathArg += ":" + dockerPath;
			}
			dockerImage.withFileFromPath("bin/server/target/mavendependencies-sharedlibs", libFolder.toPath());

			// Add sudoers
			dockerImage.withFileFromString("sudoers", "root ALL=(ALL) ALL\n%mesh ALL=(ALL) NOPASSWD: ALL\n");

			// Add docker file which contains the build instructions
			String dockerFile = IOUtils.toString(MeshContainer.class.getResourceAsStream("/Dockerfile.local"));

			// We need to keep the uid of the docker container env and the local test execution env in sync to be able to access the data of the mounted volume.
			int uid = UnixUtils.getUid();
			dockerFile = dockerFile.replace("%UID%", String.valueOf(uid));
			dockerFile = dockerFile.replace("%CMD%", generateCommand(classPathArg));
			dockerImage.withFileFromString("Dockerfile", dockerFile);

			// Add custom mesh.yml
			String yaml = generateMeshYML(enableClustering);
			dockerImage.withFileFromString("/mesh.yml", yaml);

			return dockerImage;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String generateMeshYML(boolean enableClustering) throws JsonProcessingException {
		MeshOptions options = new MeshOptions();
		options.getClusterOptions().setEnabled(enableClustering);
		options.getClusterOptions().setVertxPort(8600);
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		return OptionsLoader.getYAMLMapper().writeValueAsString(options);
	}

	private static JsonObject generateDistributedConfig(int writeQuorum) {
		JsonObject json;
		try {
			json = new JsonObject(IOUtils.toString(MeshContainer.class.getResourceAsStream("/config/default-distributed-db-config.json")));
			if (writeQuorum == -1) {
				json.put("writeQuorum", "majority");
			} else {
				json.put("writeQuorum", writeQuorum);
			}
			return json;
		} catch (IOException e) {
			throw new RuntimeException("Could not find default config", e);
		}

	}

	private static String generateCommand(String classpath) {
		StringBuilder builder = new StringBuilder();
		builder.append("exec");
		builder.append(" ");
		builder.append("java");
		builder.append(" ");
		builder.append("$JAVAOPTS");
		builder.append(" ");
		builder.append("-cp");
		builder.append(" ");
		builder.append(classpath);
		builder.append(" ");
		builder.append("com.gentics.mesh.server.ServerRunner");
		return builder.toString();
	}

	/**
	 * Block until the startup message has been seen in the container log output.
	 * 
	 * @param timeoutInSeconds
	 * @throws InterruptedException
	 * @return Fluent API
	 */
	public MeshContainer awaitStartup(int timeoutInSeconds) throws InterruptedException {
		startupConsumer.await(timeoutInSeconds, SECONDS);
		return this;
	}

	public MeshRestClient client() {
		return client;
	}

	/**
	 * Drop traffic to all or a specific set of containers.
	 * 
	 * @param containers
	 * @return
	 * @throws Exception
	 */
	public MeshContainer dropTraffic(MeshContainer... containers) throws Exception {
		execRootInContainer("apk", "--update", "add", "iptables");
		Thread.sleep(1000);
		if (containers.length == 0) {
			execRootInContainer("iptables", "-P", "INPUT", "DROP");
			execRootInContainer("iptables", "-P", "OUTPUT", "DROP");
			execRootInContainer("iptables", "-P", "FORWARD", "DROP");
		} else {
			for (MeshContainer container : containers) {
				execRootInContainer("iptables", "-I", "INPUT", "1", "-d", container.getInternalContainerIpAddress(), "-j", "DROP");
				execRootInContainer("iptables", "-I", "OUTPUT", "1", "-d", container.getInternalContainerIpAddress(), "-j", "DROP");
				execRootInContainer("iptables", "-I", "FORWARD", "1", "-d", container.getInternalContainerIpAddress(), "-j", "DROP");
			}
		}
		return this;
	}

	/**
	 * Resume traffic to all or a specific set of containers.
	 * 
	 * @param containers
	 * @return
	 * @throws Exception
	 */
	public MeshContainer resumeTraffic(MeshContainer... containers) throws Exception {
		execRootInContainer("apk", "--update", "add", "iptables");
		if (containers.length == 0) {
			execRootInContainer("iptables", "-F");
		} else {
			for (MeshContainer container : containers) {
				execRootInContainer("iptables", "-I", "INPUT", "1", "-d", container.getInternalContainerIpAddress(), "-j", "ACCEPT");
				execRootInContainer("iptables", "-I", "OUTPUT", "1", "-d", container.getInternalContainerIpAddress(), "-j", "ACCEPT");
				execRootInContainer("iptables", "-I", "FORWARD", "1", "-d", container.getInternalContainerIpAddress(), "-j", "ACCEPT");
			}
		}
		return this;
	}

	public ExecResult execRootInContainer(String... command) throws Exception {
		Charset outputCharset = UTF8;

		if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
			// at time of writing, this is the expected result in CircleCI.
			throw new UnsupportedOperationException("Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");
		}

		if (!isRunning()) {
			throw new IllegalStateException("Container is not running so exec cannot be run");
		}

		this.dockerClient.execCreateCmd(this.getContainerId()).withCmd(command);

		logger().debug("Running \"exec\" command: " + String.join(" ", command));
		final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(this.getContainerId()).withAttachStdout(true).withAttachStderr(true)
			.withUser("root")
			.withPrivileged(true)
			.withCmd(command).exec();

		final ToStringConsumer stdoutConsumer = new ToStringConsumer();
		final ToStringConsumer stderrConsumer = new ToStringConsumer();

		FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
		callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
		callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

		dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();

//		Integer exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCode();
//
//		final ExecResult result = new ExecResult(exitCode, stdoutConsumer.toString(outputCharset), stderrConsumer.toString(outputCharset));
//
//		logger().trace("stdout: " + result.getStdout());
//		logger().trace("stderr: " + result.getStderr());
//		return result;
		return null;
	}

	public MeshContainer login() {
		client().setLogin("admin", "admin");
		client().login().blockingGet();
		return this;
	}

	/**
	 * Expose the debug port to connect to.
	 * 
	 * @param debugPort
	 *            JNLP debug port. No debugging is enabled when set to null.
	 * @return Fluent API
	 */
	public MeshContainer withDebug(int debugPort) {
		this.debugPort = debugPort;
		return this;
	}

	/**
	 * Wait until the mesh instance is ready.
	 * 
	 * @return
	 */
	public MeshContainer waitForStartup() {
		waitForStartup = true;
		return this;
	}

	/**
	 * Use the provided JVM arguments.
	 * 
	 * @param opts
	 *            Additional JVM options }
	 * @return
	 */
	public MeshContainer withExtraOpts(String opts) {
		extraOpts = opts;
		return this;
	}

	/**
	 * Set the name of the node.
	 * 
	 * @param name
	 * @return
	 */
	public MeshContainer withNodeName(String name) {
		this.nodeName = name;
		return this;
	}

	/**
	 * Set the name of the cluster.
	 * 
	 * @param name
	 * @return
	 */
	public MeshContainer withClusterName(String name) {
		this.clusterName = name;
		return this;
	}

	/**
	 * Start the embedded ES
	 * 
	 * @return
	 */
	public MeshContainer withES() {
		this.startEmbeddedES = true;
		return this;
	}

	/**
	 * Set the init cluster flag.
	 * 
	 * @return
	 */
	public MeshContainer withInitCluster() {
		this.initCluster = true;
		return this;
	}

	public MeshContainer withCoordinatorPlane() {
		return withCoordinatorPlane(CoordinatorMode.ALL);
	}

	public MeshContainer withCoordinatorPlane(CoordinatorMode coordinatorMode) {
		this.coordinatorMode = coordinatorMode;
		return this;
	}

	public MeshContainer withCoordinatorRegex(String regex) {
		this.coordinatorPlaneRegex = regex;
		return this;
	}

	/**
	 * Clear the data folder during startup.
	 * 
	 * @return
	 */
	public MeshContainer withClearFolders() {
		this.clearDataFolders = true;
		return this;
	}

	/**
	 * Run the mesh server with file system persisting enabled.
	 * 
	 * @return
	 */
	public MeshContainer withFilesystem() {
		this.useFilesystem = true;
		return this;
	}
	
	public MeshContainer overrideODBClusterBackupFolder(String folder, BindMode bindMode) {
		ContainerPath override = this.pathOverrides.get(PATH_BACKUP);
		override.hostPath = folder;
		override.bindMode = bindMode;
		return this;
	}
	
	public MeshContainer overrideGraphDbFolder(String folder, BindMode bindMode) {
		ContainerPath override = this.pathOverrides.get(PATH_GRAPHDB);
		override.hostPath = folder;
		override.bindMode = bindMode;
		return this;
	}
	
	public MeshContainer overrideUploadsFolder(String folder, BindMode bindMode) {
		ContainerPath override = this.pathOverrides.get(PATH_UPLOADS);
		override.hostPath = folder;
		override.bindMode = bindMode;
		return this;
	}

	/**
	 * Set the data path postfix. This will append the postfix to all data folders in order to make them unique.
	 * 
	 * @param postfix
	 * @return
	 */
	public MeshContainer withDataPathPostfix(String postfix) {
		this.dataPathPostfix = postfix;
		return this;
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getDataPathPostfix() {
		return dataPathPostfix;
	}

	public String getClusterName() {
		return clusterName;
	}

	@Override
	public String getContainerIpAddress() {
		String containerHost = System.getenv("CONTAINER_HOST");
		if (containerHost != null) {
			return containerHost;
		} else {
			return "127.0.0.1";
		}
	}

	public String getHost() {
		return getContainerIpAddress();
	}

	public int getPort() {
		return getMappedPort(8080);
	}

	public String getInternalContainerIpAddress() {
		return getContainerInfo().getNetworkSettings().getIpAddress();
	}

	public MeshContainer withWriteQuorum(int writeQuorum) {
		this.writeQuorum = writeQuorum;
		return this;
	}

	public MeshContainer withPlugin(File file, String targetFileName) {
		if (!file.exists()) {
			fail("The provided plugin file {" + file + "} does not exist.");
		}
		addFileSystemBind(file.getAbsolutePath(), "/plugins/" + targetFileName, BindMode.READ_ONLY);
		return this;
	}

	public MeshContainer withPluginTimeout(int timeoutInSeconds) {
		addEnv(MeshOptions.MESH_PLUGIN_TIMEOUT_ENV, String.valueOf(timeoutInSeconds));
		return this;
	}

	public MeshContainer withPublicKeys(File file) {
		addFileSystemBind(file.getAbsolutePath(), "/config/public-keys.json", BindMode.READ_ONLY);
		return this;
	}

	private boolean isClustered() {
		return clusterName != null;
	}
	
	protected class ContainerPath {
		String hostPath;
		BindMode bindMode;
		
		/**
		 * Container path constructor
		 * 
		 * @param hostPath set an empty string or null for a default value, or provide an initialized and accessible folder otherwise
		 * @param bindMode 
		 */
		public ContainerPath(String hostPath, BindMode bindMode) {
			this.hostPath = hostPath;
			this.bindMode = bindMode;
		}
	}

}