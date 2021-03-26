package com.gentics.mesh.test.docker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.TestEnvironment;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static final String VERSION_ES7 = "7.4.0";
	
	public static final String VERSION_ES6 = "6.8.1";

	public ElasticsearchContainer() {
		this(VERSION_ES6);
	}

	public ElasticsearchContainer(String version) {
		super("docker.apa-it.at/elasticsearch/elasticsearch-oss:" + version);
	}

	@Override
	protected void configure() {
		addEnv("discovery.type", "single-node");
		withTmpFs(Collections.singletonMap("/usr/share/elasticsearch/data", "rw,size=64m"));
		// addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofSeconds(30L));
		waitingFor(new HttpWaitStrategy().forPath("/").withStartupTimeout(Duration.ofMinutes(2)));
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

	public ExecResult execRootInContainer(String... command) throws UnsupportedOperationException, IOException, InterruptedException {
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

//		final ExecResult result = new ExecResult(stdoutConsumer.toString(outputCharset), stderrConsumer.toString(outputCharset));
//
//		logger().debug("stdout: " + result.getStdout());
//		logger().debug("stderr: " + result.getStderr());
//		return result;
		return null;
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

}
