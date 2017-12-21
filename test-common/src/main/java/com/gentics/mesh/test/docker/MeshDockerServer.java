package com.gentics.mesh.test.docker;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

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

	/**
	 * Action which will be invoked once the mesh instance is ready.
	 */
	private Action0 startupAction = () -> {
		client = MeshRestClient.create(getContainerIpAddress(), getMappedPort(8080), false, vertx);
		client.setLogin("admin", "admin");
		client.login().toBlocking().value();
	};
	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node.
	 */
	private String nodeName;

	private boolean waitForStartup;

	private Integer debugPort;

	private String extraOpts;

	/**
	 * Create a new docker server
	 * 
	 * @param nodeName
	 * @param waitForStartup
	 * @param vertx
	 *            Vertx instances used to create the rest client
	 * @param debugPort
	 *            JNLP debug port. No debugging is enabled when set to null.
	 * @param extraOpts
	 *            Additional JVM options
	 */
	public MeshDockerServer(String meshImage, String nodeName, boolean waitForStartup, Vertx vertx, Integer debugPort, String extraOpts) {
		super(meshImage);
		this.vertx = vertx;
		this.nodeName = nodeName;
		this.waitForStartup = waitForStartup;
		this.debugPort = debugPort;
		this.extraOpts = extraOpts;
		setWaitStrategy(new NoWaitStrategy());
	}

	@Override
	protected void configure() {
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv("NODENAME", nodeName);
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

		exposedPorts.add(8080);
		exposedPorts.add(9200);
		exposedPorts.add(9300);

		setExposedPorts(exposedPorts);
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

	private static String generateMeshYML(boolean enableClustering) throws JsonProcessingException {
		MeshOptions options = new MeshOptions();
		options.getClusterOptions().setEnabled(enableClustering);
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		return OptionsLoader.getYAMLMapper().writeValueAsString(options);
	}

	private static String generateRunScript(String classpath) {
		// TODO Add an automatic shutdown timer to prevent dangling docker containers
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/sh\n");
		builder.append("java $JAVAOPTS -cp " + classpath + " com.gentics.mesh.server.ServerRunner");
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
