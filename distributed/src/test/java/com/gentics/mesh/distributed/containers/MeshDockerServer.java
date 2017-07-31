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
import org.testcontainers.containers.ContainerLaunchException;
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
public class MeshDockerServer<SELF extends MeshDockerServer<SELF>> extends GenericContainer<SELF> {

	private static final Logger log = LoggerFactory.getLogger(MeshDockerServer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	private static ImageFromDockerfile image = prepareDockerImage(true);

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
	 * Name of the node.
	 */
	private String nodeName;

	private boolean initCluster;

	private boolean waitForStartup;

	public MeshDockerServer(String prefix) {
		this(prefix, false, true);
	}

	public MeshDockerServer(String nodeName, boolean initCluster, boolean waitForStartup) {
		super(image);
		this.initCluster = initCluster;
		this.nodeName = nodeName;
		this.waitForStartup = waitForStartup;
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
		//setNetworkMode("host");
		addEnv("NODENAME", nodeName);
		setExposedPorts(Arrays.asList(8080));
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
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
		options.setClusterMode(enableClustering);
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		return OptionsLoader.getYAMLMapper().writeValueAsString(options);
	}

	private static String generateRunScript(String classpath) {
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n");
		// builder.append("echo java -cp " + classpath + " com.gentics.mesh.server.ServerRunner -nodeName $NODENAME $MESHARGS\n");
		builder.append("java -cp " + classpath + " com.gentics.mesh.server.ServerRunner -nodeName $NODENAME $MESHARGS\n");
		// builder.append("java -Dfile.encoding=UTF-8 -classpath
		// bin/server/target/classes:bin/core/target/classes:bin/common/target/classes:bin/server/target/mavendependencies-sharedlibs/guava-20.0-rc1.jar:bin/server/target/mavendependencies-sharedlibs/ferma-2.2.2.jar:bin/server/target/mavendependencies-sharedlibs/gson-2.8.1.jar:bin/server/target/mavendependencies-sharedlibs/gremlin-java-2.6.0.jar:bin/server/target/mavendependencies-sharedlibs/pipes-2.6.0.jar:bin/server/target/mavendependencies-sharedlibs/reflections-0.9.11.jar:bin/server/target/mavendependencies-sharedlibs/javassist-3.21.0-GA.jar:bin/server/target/mavendependencies-sharedlibs/vertx-rx-java-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/httpcore-4.2.5.jar:bin/server/target/mavendependencies-sharedlibs/java-uuid-generator-3.1.4.jar:bin/server/target/mavendependencies-sharedlibs/jackson-dataformat-yaml-2.8.3.jar:bin/server/target/mavendependencies-sharedlibs/snakeyaml-1.15.jar:bin/server/target/mavendependencies-sharedlibs/rxjava-debug-1.0.3.jar:bin/server/target/mavendependencies-sharedlibs/rxjava-1.2.3.jar:bin/api/target/classes:bin/rest-model/target/classes:bin/server/target/mavendependencies-sharedlibs/jackson-annotations-2.8.3.jar:bin/server/target/mavendependencies-sharedlibs/jackson-module-jsonSchema-2.4.4.jar:bin/server/target/mavendependencies-sharedlibs/jettison-1.3.8.jar:bin/server/target/mavendependencies-sharedlibs/stax-api-1.0.1.jar:bin/server/target/mavendependencies-sharedlibs/vertx-auth-common-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/vertx-auth-jwt-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/vertx-jwt-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/spring-security-core-3.2.1.RELEASE.jar:bin/server/target/mavendependencies-sharedlibs/raml-parser-0.8.17.jar:bin/server/target/mavendependencies-sharedlibs/commons-lang-2.6.jar:bin/server/target/mavendependencies-sharedlibs/commons-beanutils-1.9.2.jar:bin/server/target/mavendependencies-sharedlibs/json-schema-validator-2.2.6.jar:bin/server/target/mavendependencies-sharedlibs/jsr305-3.0.0.jar:bin/server/target/mavendependencies-sharedlibs/libphonenumber-6.2.jar:bin/server/target/mavendependencies-sharedlibs/json-schema-core-1.2.5.jar:bin/server/target/mavendependencies-sharedlibs/uri-template-0.9.jar:bin/server/target/mavendependencies-sharedlibs/msg-simple-1.1.jar:bin/server/target/mavendependencies-sharedlibs/btf-1.2.jar:bin/server/target/mavendependencies-sharedlibs/jackson-coreutils-1.8.jar:bin/server/target/mavendependencies-sharedlibs/mailapi-1.4.3.jar:bin/server/target/mavendependencies-sharedlibs/jopt-simple-4.6.jar:bin/server/target/mavendependencies-sharedlibs/juniversalchardet-1.0.3.jar:bin/server/target/mavendependencies-sharedlibs/commons-validator-1.6.jar:bin/server/target/mavendependencies-sharedlibs/commons-digester-1.8.1.jar:bin/server/target/mavendependencies-sharedlibs/commons-logging-1.2.jar:bin/elasticsearch/target/classes:bin/server/target/mavendependencies-sharedlibs/elasticsearch-2.4.4.jar:bin/server/target/mavendependencies-sharedlibs/lucene-core-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-backward-codecs-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-analyzers-common-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-queries-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-memory-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-highlighter-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-queryparser-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-sandbox-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-suggest-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-misc-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-join-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-grouping-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-spatial-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/lucene-spatial3d-5.5.2.jar:bin/server/target/mavendependencies-sharedlibs/spatial4j-0.5.jar:bin/server/target/mavendependencies-sharedlibs/securesm-1.0.jar:bin/server/target/mavendependencies-sharedlibs/jackson-dataformat-smile-2.8.1.jar:bin/server/target/mavendependencies-sharedlibs/jackson-dataformat-cbor-2.8.1.jar:bin/server/target/mavendependencies-sharedlibs/netty-3.10.6.Final.jar:bin/server/target/mavendependencies-sharedlibs/compress-lzf-1.0.2.jar:bin/server/target/mavendependencies-sharedlibs/t-digest-3.0.jar:bin/server/target/mavendependencies-sharedlibs/HdrHistogram-2.1.6.jar:bin/server/target/mavendependencies-sharedlibs/jsr166e-1.1.0.jar:bin/server/target/mavendependencies-sharedlibs/delete-by-query-2.4.4.jar:bin/server/target/mavendependencies-sharedlibs/hppc-0.7.1.jar:bin/server/target/mavendependencies-sharedlibs/lucene-expressions-4.10.4.jar:bin/server/target/mavendependencies-sharedlibs/antlr-runtime-3.5.jar:bin/server/target/mavendependencies-sharedlibs/asm-4.1.jar:bin/server/target/mavendependencies-sharedlibs/asm-commons-4.1.jar:bin/server/target/mavendependencies-sharedlibs/groovy-all-2.4.4.jar:bin/verticles/graphql/target/classes:bin/server/target/mavendependencies-sharedlibs/graphql-java-3.0.0.jar:bin/server/target/mavendependencies-sharedlibs/antlr4-runtime-4.5.1.jar:bin/rest-client/target/classes:bin/server/target/mavendependencies-sharedlibs/commons-codec-1.10.jar:bin/server/target/mavendependencies-sharedlibs/logback-classic-1.1.2.jar:bin/server/target/mavendependencies-sharedlibs/logback-core-1.1.2.jar:bin/server/target/mavendependencies-sharedlibs/slf4j-api-1.7.7.jar:bin/server/target/mavendependencies-sharedlibs/commons-cli-1.2.jar:bin/server/target/mavendependencies-sharedlibs/commons-lang3-3.3.2.jar:bin/services/image-imgscalr/target/classes:bin/server/target/mavendependencies-sharedlibs/imgscalr-lib-4.2.jar:bin/server/target/mavendependencies-sharedlibs/tika-parsers-0.6.jar:bin/server/target/mavendependencies-sharedlibs/tika-core-0.6.jar:bin/server/target/mavendependencies-sharedlibs/commons-compress-1.0.jar:bin/server/target/mavendependencies-sharedlibs/pdfbox-0.8.0-incubating.jar:bin/server/target/mavendependencies-sharedlibs/fontbox-0.8.0-incubator.jar:bin/server/target/mavendependencies-sharedlibs/jempbox-0.8.0-incubator.jar:bin/server/target/mavendependencies-sharedlibs/poi-3.6.jar:bin/server/target/mavendependencies-sharedlibs/poi-scratchpad-3.6.jar:bin/server/target/mavendependencies-sharedlibs/poi-ooxml-3.6.jar:bin/server/target/mavendependencies-sharedlibs/poi-ooxml-schemas-3.6.jar:bin/server/target/mavendependencies-sharedlibs/xmlbeans-2.3.0.jar:bin/server/target/mavendependencies-sharedlibs/dom4j-1.6.1.jar:bin/server/target/mavendependencies-sharedlibs/xml-apis-1.0.b2.jar:bin/server/target/mavendependencies-sharedlibs/geronimo-stax-api_1.0_spec-1.0.1.jar:bin/server/target/mavendependencies-sharedlibs/tagsoup-1.2.jar:bin/server/target/mavendependencies-sharedlibs/asm-3.1.jar:bin/server/target/mavendependencies-sharedlibs/log4j-1.2.14.jar:bin/server/target/mavendependencies-sharedlibs/metadata-extractor-2.4.0-beta-1.jar:bin/server/target/mavendependencies-sharedlibs/imageio-jpeg-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/imageio-core-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/imageio-metadata-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/common-lang-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/common-io-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/common-image-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/imageio-tiff-3.3.2.jar:bin/server/target/mavendependencies-sharedlibs/imageio-icns-3.3.2.jar:bin/changelog-system/target/classes:bin/server/target/mavendependencies-sharedlibs/vertx-core-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/netty-common-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-buffer-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-transport-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-handler-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-codec-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-handler-proxy-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-codec-socks-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-codec-http-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-codec-http2-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-resolver-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-resolver-dns-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/netty-codec-dns-4.1.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/jackson-core-2.7.4.jar:bin/server/target/mavendependencies-sharedlibs/jackson-databind-2.7.4.jar:bin/server/target/mavendependencies-sharedlibs/vertx-codegen-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/mvel2-2.2.8.Final.jar:bin/server/target/mavendependencies-sharedlibs/vertx-hazelcast-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/hazelcast-3.6.3.jar:bin/server/target/mavendependencies-sharedlibs/vertx-mail-client-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/vertx-web-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/caffeine-2.3.3.jar:bin/server/target/mavendependencies-sharedlibs/dagger-2.7.jar:bin/server/target/mavendependencies-sharedlibs/javax.inject-1.jar:bin/server/target/mavendependencies-sharedlibs/vertx-dropwizard-metrics-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/metrics-core-3.1.2.jar:bin/server/target/mavendependencies-sharedlibs/ferma-extensions-api-0.1.1.jar:bin/server/target/mavendependencies-sharedlibs/joda-time-2.9.6.jar:bin/server/target/mavendependencies-sharedlibs/mockito-all-1.10.19.jar:bin/verticles/admin-gui/target/classes:bin/server/target/mavendependencies-sharedlibs/mesh-admin-ui-0.7.1-dist.jar:bin/server/target/mavendependencies-sharedlibs/vertx-web-templ-handlebars-3.4.2.jar:bin/server/target/mavendependencies-sharedlibs/handlebars-4.0.3.jar:bin/server/target/mavendependencies-sharedlibs/rhino-1.7R4.jar:bin/databases/orientdb/target/classes:bin/server/target/mavendependencies-sharedlibs/orientdb-core-2.2.24.jar:bin/server/target/mavendependencies-sharedlibs/snappy-java-1.1.0.1.jar:bin/server/target/mavendependencies-sharedlibs/concurrentlinkedhashmap-lru-1.4.1.jar:bin/server/target/mavendependencies-sharedlibs/orientdb-graphdb-2.2.24.jar:bin/server/target/mavendependencies-sharedlibs/orientdb-server-2.2.24.jar:bin/server/target/mavendependencies-sharedlibs/orientdb-client-2.2.24.jar:bin/server/target/mavendependencies-sharedlibs/mail-1.4.7.jar:bin/server/target/mavendependencies-sharedlibs/activation-1.1.jar:bin/server/target/mavendependencies-sharedlibs/orientdb-tools-2.2.24.jar:bin/server/target/mavendependencies-sharedlibs/commons-collections-3.2.2.jar:bin/server/target/mavendependencies-sharedlibs/blueprints-core-2.6.0.jar:bin/server/target/mavendependencies-sharedlibs/commons-configuration-1.6.jar:bin/server/target/mavendependencies-sharedlibs/commons-beanutils-core-1.8.0.jar:bin/server/target/mavendependencies-sharedlibs/orientdb-distributed-2.2.24.jar:bin/server/target/mavendependencies-sharedlibs/hazelcast-cloud-3.6.5.jar:bin/server/target/mavendependencies-sharedlibs/commons-io-2.4.jar:bin/server/target/mavendependencies-sharedlibs/ferma-extensions-orientdb-0.1.1.jar
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
