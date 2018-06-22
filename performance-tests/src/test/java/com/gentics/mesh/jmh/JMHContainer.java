package com.gentics.mesh.jmh;

import java.io.File;
import java.io.IOException;
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

	public static LazyFuture<String> prepareDockerImage() {
		ImageFromDockerfile dockerImage = new ImageFromDockerfile("jmh-container", true);
		dockerImage.withDockerfileFromBuilder(builder -> {
			builder.from("maven:3.5-jdk-9")
				.add(".", "/maven")
				.workDir("/maven")
				.build();
		}).withFileFromFile("/", new File(".."));
		return dockerImage;
	}

	/**
	 * Create a new JMH container which will run the tests
	 * 
	 * @param name
	 *            Name of the benchmark run
	 */
	public JMHContainer(String name) {
		super(prepareDockerImage());
		setWaitStrategy(Wait.forLogMessage("SUCCESS", 1));
		this.name = name;
	}

	@Override
	protected void configure() {
		withStartupTimeout(Duration.ofMinutes(15));
		withWorkingDirectory("/maven");
		// 20 Seconds is enough to potentially download the result files.
		withCommand("bash", "-c", "mvn -B -Djmh.name=" + name + " test-compile exec:exec && sleep 20");
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