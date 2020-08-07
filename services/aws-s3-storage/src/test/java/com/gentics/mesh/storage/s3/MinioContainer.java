package com.gentics.mesh.storage.s3;

import java.net.URI;
import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

public class MinioContainer extends GenericContainer<MinioContainer> {

	public static final String VERSION = "7268-b336f52";

	public static final String MINIO_ACCESS_KEY = "minio";

	public static final String MINIO_SECRET_KEY = "miniosecret";

	public MinioContainer() {
		super("minio/minio:" + VERSION);
	}

	@Override
	protected void configure() {
		withCommand("server /data");
		withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY);
		withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY);
		withExposedPorts(9000);
		withLogConsumer(o -> System.out.print(o.getUtf8String()));
		withStartupTimeout(Duration.ofSeconds(10L));
		waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready"));
	}

	/**
	 * Return the server address.
	 * 
	 * @return
	 */
	public URI getURI() {
		return URI.create("http://"
			+ getContainerIpAddress()
			+ ":" + getFirstMappedPort());
	}

}
