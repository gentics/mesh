package com.gentics.mesh.jmh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.LazyFuture;

public class JMHContainer extends GenericContainer<JMHContainer> {

	private static final Logger log = LoggerFactory.getLogger(JMHContainer.class);

	private MavenBuildLock buildLock = new MavenBuildLock();
	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private String name;

	public static LazyFuture<String> prepareDockerImage(String mavenVersion) {
		ImageFromDockerfile dockerImage = new ImageFromDockerfile("jmh-container", true);
		dockerImage.withDockerfileFromBuilder(builder -> {
			builder.from("maven:" + mavenVersion)
				.add(".", "/maven")
				.workDir("/maven")
				.build();
		});

		try {
			Files.walk(Paths.get(".."))
				.filter(Files::isRegularFile)
				.filter(f -> !f.toString().contains("/target/"))
				.filter(f -> !f.toString().contains("/services/image-imgscalr/src/test/resources"))
				.filter(f -> !f.toString().contains("/elasticsearch/src/main/resources"))
				.filter(f -> !f.toString().contains("/databases/orientdb/src/main/resources"))
				.filter(f -> !f.toString().contains("/common/src/main/resources/"))
				.filter(f -> !f.toString().contains("/verticles/graphql/src/main/resources/"))
				.filter(f -> !f.toString().contains("/core/src/test/resources"))
				.filter(f -> !f.toString().contains("/demo/src/main/resources"))
				.filter(f -> !f.toString().contains("/doc/src/main/docs/"))
				.filter(f -> !f.toString().contains("/doc/src/main/resources/"))
				.filter(f -> !f.toString().contains("/server/elasticsearch/"))
				.filter(f -> !f.toString().contains("/demo/elasticsearch/"))
				.filter(f -> !f.toString().startsWith("../core/plugins/"))
				.filter(f -> !f.toString().startsWith("../demo/data/"))
				.filter(f -> !f.toString().startsWith("../server/data/"))
				.filter(f -> !f.toString().startsWith("../core/data/"))
				.filter(f -> !f.toString().contains("/.testcontainers"))
				.filter(f -> !f.toString().contains("/.project"))
				.filter(f -> !f.toString().contains("/.git/"))
				.filter(f -> !f.toString().contains("/.github"))
				.filter(f -> !f.toString().contains("/.idea"))
				.filter(f -> !f.toString().contains("/.classpath"))
				.filter(f -> !f.toString().contains("/.settings"))
				.filter(f -> !f.toString().contains("/.travis"))
				.filter(f -> !f.toString().contains("/.jenkins"))
				.filter(f -> !f.toString().contains("/Jenkinsfile"))
				.filter(f -> !f.toString().contains("/.gitignore"))
				.filter(f -> !f.toString().contains("/.factorypath"))
				.filter(f -> !f.toString().contains("/.dockerignore"))
				.filter(f -> !f.toString().contains("/.gitattributes"))
				.filter(f -> !f.toString().endsWith(".iml"))
				.filter(f -> !f.toString().endsWith(".bak"))
				.filter(f -> !f.toString().endsWith(".orig"))
				.forEach(file -> {
					String path = file.toString();
					path = path.substring(2);
					dockerImage.withFileFromFile(path, file.toFile());
				});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dockerImage;
	}

	/**
	 * Create a new JMH container which will run the tests
	 * 
	 * @param name
	 *            Name of the benchmark run
	 * @param mavenVersion
	 * 
	 */
	public JMHContainer(String name, String mavenVersion) {
		super(prepareDockerImage(mavenVersion));
		setWaitStrategy(Wait.forLogMessage("SUCCESS", 1));
		this.name = name;
	}

	@Override
	protected void configure() {
		withStartupTimeout(Duration.ofMinutes(15));
		withWorkingDirectory("/maven");
		// 20 Seconds is enough to potentially download the result files.
		withCommand("bash", "-c", "mvn -B -Djmh.name=" + name + " -Djmh.skip=false test-compile -pl '!demo,!doc,!server' && sleep 20");
		setStartupAttempts(1);
		waitingFor(new NoWaitStrategy());
		setLogConsumers(Arrays.asList(logConsumer, buildLock));
	}

	@Override
	public void start() {
		super.start();
		try {
			buildLock.await(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			throw new ContainerLaunchException("Container build did not not finish on-time", e);
		}
	}

	/**
	 * Download the JMH result to the client.
	 * 
	 * @param targetPath
	 * @return Fluent API
	 * @throws IOException
	 */
	public JMHContainer downloadResult(String targetPath) throws IOException {
		copyFileFromContainer("/maven/target/results/" + name + ".json", targetPath);
		return this;
	}

}

class MavenBuildLock implements Consumer<OutputFrame> {

	private CountDownLatch latch = new CountDownLatch(1);

	private static Logger log = LoggerFactory.getLogger(MavenBuildLock.class);

	@Override
	public void accept(OutputFrame frame) {
		if (frame != null) {
			String utf8String = frame.getUtf8String();
			if (utf8String.contains("SUCCESS")) {
				log.info("Build finished. Releasing lock");
				latch.countDown();
			}
		}
	}

	public void await(int value, TimeUnit unit) throws InterruptedException {
		if (!latch.await(value, unit)) {
			throw new RuntimeException("Build did not finish in time.");
		}
	}
}