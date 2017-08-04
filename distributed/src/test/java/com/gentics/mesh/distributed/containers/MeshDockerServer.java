package com.gentics.mesh.distributed.containers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Vertx;
import rx.functions.Action0;

/**
 * Test container for a mesh instance which uses local class files. The image for the container will automatically be rebuild during each startup.
 * 
 * @param <SELF>
 */
public class MeshDockerServer<SELF extends MeshDockerServer<SELF>> extends GenericContainer<SELF> {

	private static final Logger log = LoggerFactory.getLogger(MeshDockerServer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	private Vertx vertx;

	private static ImageFromDockerfile image = prepareDockerImage(true);

	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */
	private Action0 startupAction = () -> {
		client = MeshRestClient.create("localhost", getMappedPort(8080), vertx);
		client.setLogin("admin", "admin");
		client.login().toBlocking().value();
	};
	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node.
	 */
	private String nodeName;

	private boolean initCluster;

	private boolean waitForStartup;

	private Integer debugPort;

	public MeshDockerServer(String prefix) {
		this(prefix, false, true, Vertx.vertx(), null);
	}

	/**
	 * Create a new docker server
	 * 
	 * @param nodeName
	 * @param initCluster
	 * @param waitForStartup
	 * @param vertx
	 *            Vertx instances used to create the rest client
	 */
	public MeshDockerServer(String nodeName, boolean initCluster, boolean waitForStartup, Vertx vertx, Integer debugPort) {
		super(image);
		this.vertx = vertx;
		this.initCluster = initCluster;
		this.nodeName = nodeName;
		this.waitForStartup = waitForStartup;
		this.debugPort = debugPort;
	}

	@Override
	protected void configure() {
		String dataPath = "target/" + nodeName + "-data";

		try {
			prepareFolder(dataPath);
		} catch (Exception e) {
			fail("Could not setup bind folders");
		}

		addFileSystemBind(dataPath, "/data", BindMode.READ_WRITE);
		if (initCluster) {
			addEnv("MESHARGS", "-initCluster");
		}
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv("NODENAME", nodeName);
		if (debugPort != null) {
			addEnv("JAVAOPTS", "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n ");
			exposedPorts.add(8000);
			setPortBindings(Arrays.asList("8000:8000"));
		}
		exposedPorts.add(8080);
		exposedPorts.add(9200);
		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
//		setContainerName("mesh-test-" + nodeName);
		setStartupAttempts(1);
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

			// Add run script which executes mesh
			dockerImage.withFileFromString("run.sh", generateRunScript(classPathArg));

			// Add docker file which contains the build instructions
			dockerImage.withFileFromClasspath("Dockerfile", "/Dockerfile.local");

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
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		options.getSearchOptions().setHttpEnabled(true);
		return OptionsLoader.getYAMLMapper().writeValueAsString(options);
	}

	private static String generateRunScript(String classpath) {
		// TODO Add an automatic shutdown timer to prevent dangling docker containers
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n");
		// builder.append("echo java -cp " + classpath + " com.gentics.mesh.server.ServerRunner -nodeName $NODENAME $MESHARGS\n");
		builder.append("java $JAVAOPTS -cp " + classpath + " com.gentics.mesh.server.ServerRunner -nodeName $NODENAME $MESHARGS\n");
		// builder.append("java -Dfile.encoding=UTF-8 -classpath
		// com.gentics.mesh.server.ServerRunner -nodeName $NODENAME $MESHARGS\n");
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

	public MeshRestClient getMeshClient() {
		return client;
	}

}
