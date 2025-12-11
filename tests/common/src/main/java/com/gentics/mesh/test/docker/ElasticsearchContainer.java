package com.gentics.mesh.test.docker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestEnvironment;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static final String VERSION_ES9 = "9.1.7";

	public static final String VERSION_ES8 = "8.14.1";

	public static final String VERSION_ES7 = "7.4.0";
	
	public static final String VERSION_ES6 = "6.8.1";

	private final int majorVersion;

	public ElasticsearchContainer() {
		this(VERSION_ES6);
	}

	public ElasticsearchContainer(String version) {
		super(getImageName(version));

		majorVersion = getMajorVersion(version);
	}

	@Override
	protected void configure() {
		if (majorVersion >= 8) {
			addEnv("xpack.security.enabled", "false");
		}

		addEnv("discovery.type", "single-node");
		addEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g");
		withTmpFs(Collections.singletonMap("/usr/share/elasticsearch/data", "rw,size=64m"));
		// addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofMinutes(5));
		waitingFor(Wait.forLogMessage(".*started.*", 1).withStartupTimeout(Duration.ofMinutes(5)));
	}

	public ElasticsearchContainer dropTraffic() throws UnsupportedOperationException, IOException, InterruptedException {
		execRootInContainer("yum", "install", "-y", "iptables");
		setPolicy("DROP");
		return this;
	}

	public ElasticsearchContainer resumeTraffic() throws UnsupportedOperationException, IOException, InterruptedException {
		setPolicy("ACCEPT");
		return this;
	}

	private void setPolicy(String policy) throws UnsupportedOperationException, IOException, InterruptedException {
		execRootInContainer("iptables", "-P", "INPUT", policy);
		execRootInContainer("iptables", "-P", "OUTPUT", policy);
		execRootInContainer("iptables", "-P", "FORWARD", policy);
	}

	public void execRootInContainer(String... command) throws UnsupportedOperationException, IOException, InterruptedException {
		Charset outputCharset = UTF8;

		if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
			// at time of writing, this is the expected result in CircleCI.
			throw new UnsupportedOperationException("Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");
		}

		if (!isRunning()) {
			throw new IllegalStateException("Container is not running so exec cannot be run");
		}

		this.dockerClient.execCreateCmd(getContainerId()).withCmd(command);

		logger().debug("Running \"exec\" command: " + String.join(" ", command));
		final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(getContainerId()).withAttachStdout(true).withAttachStderr(true)
			.withUser("root")
			.withPrivileged(true)
			.withCmd(command).exec();

		final ToStringConsumer stdoutConsumer = new ToStringConsumer();
		final ToStringConsumer stderrConsumer = new ToStringConsumer();

		FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
		callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
		callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

		dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();

		logger().debug("stdout: " + stdoutConsumer.toString(outputCharset));
		logger().debug("stderr: " + stderrConsumer.toString(outputCharset));
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

	public String getHost() {
		String containerHost = System.getenv("CONTAINER_HOST");
		if (containerHost != null) {
			return containerHost;
		} else {
			return "localhost";
		}
	}

	/**
	 * Create the full image name from the version number.
	 *
	 * <p>
	 *     All versions use the docker.gentics.com repository.
	 * </p>
	 *
	 * <p>
	 *     Versions lower than Elasticsearch 8 will use the
	 *     elasticsearch-oss version of the image (an oss version
	 *     is no longer available for Elasticsearch 8).
	 * </p>
	 *
	 * @param version The Elasticsearch version to use.
	 * @return The full image name for the given version.
	 */
	private static String getImageName(String version) {
		int major = getMajorVersion(version);

		if (major >= 8) {
			return "docker.gentics.com/elasticsearch/elasticsearch:" + version;
}

		return "docker.gentics.com/elasticsearch/elasticsearch-oss:" + version;
	}

	/**
	 * Get the major version from the given version string.
	 *
	 * @param version The version string
	 * @return The major version for the given version string.
	 */
	private static int getMajorVersion(String version) {
		return Integer.parseInt(version.substring(0, version.indexOf(".")));
	}
}
