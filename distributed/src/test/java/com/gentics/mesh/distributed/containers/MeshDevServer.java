package com.gentics.mesh.distributed.containers;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.UUIDUtil;

import rx.functions.Action0;

/**
 * Test container for a mesh instance which uses local class files. The image for the container will automatically be rebuild during each startup.
 * 
 * @param <SELF>
 */
public class MeshDevServer<SELF extends MeshDevServer<SELF>> extends GenericContainer<SELF> {

	private static final Logger log = LoggerFactory.getLogger(MeshDevServer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */
	private Action0 startupAction = () -> {
		client = MeshRestClient.create("localhost", getMappedPort(8080), Mesh.vertx());
		client.setLogin("admin", "admin");
		client.login().toBlocking().value();
	};
	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Local prefix for the docker volume host mount.
	 */
	private String prefix;

	public MeshDevServer(String prefix, boolean enableClustering) {
		super(prepareDockerImage(enableClustering));
		this.prefix = prefix;
	}

	@Override
	protected void configure() {
		String dataPath = "target/" + prefix + "-data";
		// String confPath = "target/" + prefix + "-config";

		try {
			prepareFolder(dataPath);
			// prepareFolder(confPath);
		} catch (Exception e) {
			fail("Could not setup bind folders");
		}

		addFileSystemBind(dataPath, "/data", BindMode.READ_WRITE);
		// addFileSystemBind(confPath, "/config", BindMode.READ_WRITE);
		setExposedPorts(Arrays.asList(8080));
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
		setStartupAttempts(1);
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
		options.setClusterMode(enableClustering);
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		return OptionsLoader.getYAMLMapper().writeValueAsString(options);
	}

	private static String generateRunScript(String classpath) {
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n");
		builder.append("java -cp " + classpath + " com.gentics.mesh.server.ServerRunner\n\n");
		return builder.toString();
	}

	public void awaitStartup(int timeoutInSeconds) throws InterruptedException {
		startupConsumer.await(timeoutInSeconds, SECONDS);
	}

	public MeshRestClient getMeshClient() {
		return client;
	}

}
