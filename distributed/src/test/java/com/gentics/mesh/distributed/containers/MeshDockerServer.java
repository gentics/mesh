package com.gentics.mesh.distributed.containers;

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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.NoWaitStrategy;
import com.gentics.mesh.test.docker.StartupLatchingConsumer;
import com.gentics.mesh.util.UUIDUtil;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Test container for a mesh instance which uses local class files. The image for the container will automatically be rebuild during each startup.
 */
public class MeshDockerServer extends GenericContainer<MeshDockerServer> {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final Logger log = LoggerFactory.getLogger(MeshDockerServer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	private Vertx vertx;

	private static ImageFromDockerfile image = prepareDockerImage(true);

	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */
	private Runnable startupAction = () -> {
		client = MeshRestClient.create(getContainerIpAddress(), getMappedPort(8080), false);
	};

	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node.
	 */
	private String nodeName;

	/**
	 * -1 = "majority"
	 */
	private int writeQuorum = -1;

	private boolean initCluster = false;

	private boolean waitForStartup;

	private boolean clearDataFolders = false;

	private Integer debugPort;

	private String clusterName;

	private String extraOpts;

	private String dataPathPostfix;

	private boolean startEmbeddedES = false;

	/**
	 * Create a new docker server
	 * 
	 * @param vertx
	 *            Vert.x instances used to create the rest client
	 */
	public MeshDockerServer(Vertx vertx) {
		super(image);
		this.vertx = vertx;
		setWaitStrategy(new NoWaitStrategy());
	}

	@Override
	protected void configure() {
		String basePath = "/opt/jenkins-slave/" + clusterName + "-" + nodeName;
		String dataPath = basePath + "/data-" + dataPathPostfix;
		String confPath = basePath + "/config";
		// Ensure that the folder is created upfront. This is important to keep the uid and gids correct.
		// Otherwise the folder would be created by docker using root.

		if (clearDataFolders) {
			try {
				prepareFolder(dataPath);
			} catch (Exception e) {
				fail("Could not setup bind folder {" + dataPath + "}");
			}
		}
		new File(dataPath).mkdirs();
		addFileSystemBind(dataPath, "/data", BindMode.READ_WRITE);
		// withCreateContainerCmdModifier(it -> it.withVolumes(new Volume("/data")));

		try {
			new File(confPath).mkdirs();
			File confFile = new File(confPath, "default-distributed-db-config.json");
			String jsonConfig = generateDistributedConfig(writeQuorum).encodePrettily();
			FileUtils.writeStringToFile(confFile, jsonConfig, Charset.forName("UTF-8"));
			addFileSystemBind(confFile.getAbsolutePath(), "/config/default-distributed-db-config.json", BindMode.READ_ONLY);
		} catch (Exception e) {
			throw new RuntimeException("Error while creating default-distributed-db-config.json", e);
		}

		changeUserInContainer();
		if (initCluster) {
			addEnv(MeshOptions.MESH_CLUSTER_INIT_ENV, "true");
		}
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv(MeshOptions.MESH_NODE_NAME_ENV, nodeName);
		addEnv(ClusterOptions.MESH_CLUSTER_NAME_ENV, clusterName);
		addEnv(ClusterOptions.MESH_CLUSTER_VERTX_PORT_ENV, "8123");
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
		super.start();
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
		dockerClient.killContainerCmd(containerId).withSignal("SIGTERM").exec();
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
			List<Path> classFolders = Files.walk(projectRoot.toPath()).filter(file -> "classes".equals(file.toFile().getName()))
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
			String dockerFile = IOUtils.toString(MeshDockerServer.class.getResourceAsStream("/Dockerfile.local"));

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
			json = new JsonObject(IOUtils.toString(MeshDockerServer.class.getResourceAsStream("/config/default-distributed-db-config.json")));
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
	public MeshDockerServer awaitStartup(int timeoutInSeconds) throws InterruptedException {
		startupConsumer.await(timeoutInSeconds, SECONDS);
		return this;
	}

	public MeshRestClient client() {
		return client;
	}

	public MeshDockerServer dropTraffic() throws UnsupportedOperationException, IOException, InterruptedException {
		execRootInContainer("apk", "--update", "add", "iptables");
		Thread.sleep(1000);
		execRootInContainer("iptables", "-P", "INPUT", "DROP");
		execRootInContainer("iptables", "-P", "OUTPUT", "DROP");
		execRootInContainer("iptables", "-P", "FORWARD", "DROP");
		return this;
	}

	public MeshDockerServer resumeTraffic() throws UnsupportedOperationException, IOException, InterruptedException {
		execRootInContainer("iptables", "-F");
		return this;
	}

	public ExecResult execRootInContainer(String... command) throws UnsupportedOperationException, IOException, InterruptedException {
		Charset outputCharset = UTF8;

		if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
			// at time of writing, this is the expected result in CircleCI.
			throw new UnsupportedOperationException("Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");
		}

		if (!isRunning()) {
			throw new IllegalStateException("Container is not running so exec cannot be run");
		}

		this.dockerClient.execCreateCmd(this.containerId).withCmd(command);

		logger().debug("Running \"exec\" command: " + String.join(" ", command));
		final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(this.containerId).withAttachStdout(true).withAttachStderr(true)
			.withUser("root")
			// .withPrivileged(true)
			.withCmd(command).exec();

		final ToStringConsumer stdoutConsumer = new ToStringConsumer();
		final ToStringConsumer stderrConsumer = new ToStringConsumer();

		FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
		callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
		callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

		dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();

		final ExecResult result = new ExecResult(stdoutConsumer.toString(outputCharset), stderrConsumer.toString(outputCharset));

		logger().trace("stdout: " + result.getStdout());
		logger().trace("stderr: " + result.getStderr());
		return result;
	}

	public MeshDockerServer login() {
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
	public MeshDockerServer withDebug(int debugPort) {
		this.debugPort = debugPort;
		return this;
	}

	/**
	 * Wait until the mesh instance is ready.
	 * 
	 * @return
	 */
	public MeshDockerServer waitForStartup() {
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
	 * Set the name of the cluster.
	 * 
	 * @param name
	 * @return
	 */
	public MeshDockerServer withClusterName(String name) {
		this.clusterName = name;
		return this;
	}

	/**
	 * Start the embedded ES
	 * 
	 * @return
	 */
	public MeshDockerServer withES() {
		this.startEmbeddedES = true;
		return this;
	}

	/**
	 * Set the init cluster flag.
	 * 
	 * @return
	 */
	public MeshDockerServer withInitCluster() {
		this.initCluster = true;
		return this;
	}

	/**
	 * Clear the data folder during startup.
	 * 
	 * @return
	 */
	public MeshDockerServer withClearFolders() {
		this.clearDataFolders = true;
		return this;
	}

	/**
	 * Set the data path postfix.
	 * 
	 * @param postfix
	 * @return
	 */
	public MeshDockerServer withDataPathPostfix(String postfix) {
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
			return super.getContainerIpAddress();
		}
	}

	public MeshDockerServer withWriteQuorum(int writeQuorum) {
		this.writeQuorum = writeQuorum;
		return this;
	}

}
