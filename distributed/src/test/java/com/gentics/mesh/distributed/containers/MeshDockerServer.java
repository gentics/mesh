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
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.NoWaitStrategy;
import com.gentics.mesh.test.docker.StartupLatchingConsumer;
import com.gentics.mesh.util.UUIDUtil;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

import io.vertx.core.Vertx;

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
		client = MeshRestClient.create("localhost", getMappedPort(8080), false, vertx);
	};

	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node.
	 */
	private String nodeName;

	private boolean initCluster;

	private boolean waitForStartup;

	private boolean clearDataFolders;

	private Integer debugPort;

	private String clusterName;

	private String extraOpts;

	private String dataPathPostfix;

	/**
	 * Create a new docker server
	 * 
	 * @param clusterName
	 * @param nodeName
	 * @param dataPathPostfix
	 *            Postfix of the data folder
	 * @param initCluster
	 * @param waitForStartup
	 * @param vertx
	 *            Vertx instances used to create the rest client
	 * @param debugPort
	 *            JNLP debug port. No debugging is enabled when set to null.
	 * @param extraOpts
	 *            Additional JVM options
	 */
	public MeshDockerServer(String clusterName, String nodeName, String dataPathPostfix, boolean initCluster, boolean waitForStartup,
		boolean clearDataFolders, Vertx vertx, Integer debugPort, String extraOpts) {
		super(image);
		this.vertx = vertx;
		this.clusterName = clusterName;
		this.nodeName = nodeName;
		this.dataPathPostfix = dataPathPostfix;
		this.initCluster = initCluster;
		this.clearDataFolders = clearDataFolders;
		this.waitForStartup = waitForStartup;
		this.debugPort = debugPort;
		this.extraOpts = extraOpts;
		setWaitStrategy(new NoWaitStrategy());
	}

	@Override
	protected void configure() {
		String dataPath = "/opt/jenkins-slave/" + nodeName + "-data-" + dataPathPostfix;
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

		changeUserInContainer();
		if (initCluster) {
			addEnv("MESHARGS", "-" + MeshCLI.INIT_CLUSTER);
		}
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv(MeshOptions.MESH_NODE_NAME_ENV, nodeName);
		addEnv(ClusterOptions.MESH_CLUSTER_NAME_ENV, clusterName);

		// Don't run the embedded ES
		addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_START_EMBEDDED_ENV, "false");
		addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "null");

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
		exposedPorts.add(9200);
		exposedPorts.add(9300);

		// setPrivilegedMode(true);
		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
		// setContainerName("mesh-test-" + nodeName);
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
				awaitStartup(200);
			} catch (InterruptedException e) {
				throw new ContainerLaunchException("Container did not not startup on-time", e);
			}
		}
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

			// Add run script which executes mesh
			dockerImage.withFileFromString("run.sh", generateRunScript(classPathArg));

			// Add docker file which contains the build instructions
			String dockerFile = IOUtils.toString(MeshDockerServer.class.getResourceAsStream("/Dockerfile.local"));
			// We need to keep the uid of the docker container env and the local test execution env in sync to be able to access the data of the mounted volume.
			int uid = UnixUtils.getUid();
			dockerFile = dockerFile.replaceAll("%UID%", String.valueOf(uid));
			dockerImage.withFileFromString("Dockerfile", dockerFile);

			// Add custom mesh.yml
			dockerImage.withFileFromString("/mesh.yml", generateMeshYML(enableClustering));

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

	private static String generateRunScript(String classpath) {
		// TODO Add an automatic shutdown timer to prevent dangling docker containers
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n");
		builder.append("java $JAVAOPTS -cp " + classpath
			+ " com.gentics.mesh.server.ServerRunner  $MESHARGS\n");
		builder.append("\n\n");
		return builder.toString();
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

	public void dropTraffic() throws UnsupportedOperationException, IOException, InterruptedException {
		execRootInContainer("apk", "--update", "add", "iptables");
		Thread.sleep(1000);
		execRootInContainer("iptables", "-P", "INPUT", "DROP");
		execRootInContainer("iptables", "-P", "OUTPUT", "DROP");
		execRootInContainer("iptables", "-P", "FORWARD", "DROP");
	}

	public void resumeTraffic() throws UnsupportedOperationException, IOException, InterruptedException {
		execRootInContainer("iptables", "-F");
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

	public void login() {
		client().setLogin("admin", "admin").login().blockingGet();
	}

}
