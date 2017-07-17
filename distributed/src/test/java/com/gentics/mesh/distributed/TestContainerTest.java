package com.gentics.mesh.distributed;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class TestContainerTest {

	private static final Logger log = LoggerFactory.getLogger(TestContainerTest.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(50);

	@BeforeClass
	public static void cleanup() throws IOException {
		setupFolder("target/local-config");
		setupFolder("target/local-data");
		setupFolder("target/local2-config");
		setupFolder("target/local2-data");
	}

	private static void setupFolder(String path) throws IOException {
		File folder = new File(path);
		FileUtils.deleteDirectory(folder);
		folder.mkdirs();
	}

	@Rule
	public GenericContainer mesh = new GenericContainer("mesh-local:latest")

			.withFileSystemBind("target/local-data", "/data", BindMode.READ_WRITE)

			.withFileSystemBind("target/local-config", "/config", BindMode.READ_WRITE)

			.withExposedPorts(8080).withLogConsumer(logConsumer).withLogConsumer(startupConsumer);

	@Test
	public void testContainer() throws IOException, InterruptedException {
		startupConsumer.await();

		GenericContainer mesh2 = new GenericContainer("mesh-local:latest")

				.withFileSystemBind("target/local2-data", "/data", BindMode.READ_WRITE)

				.withFileSystemBind("target/local2-config", "/config", BindMode.READ_WRITE)

				.withExposedPorts(8080).withLogConsumer(logConsumer);

		mesh2.start();

		System.out.println(mesh.getContainerIpAddress() + ":" + mesh.getMappedPort(8080));
		System.in.read();
	}
}
