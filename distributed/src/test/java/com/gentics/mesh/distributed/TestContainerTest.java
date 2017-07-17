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

	@BeforeClass
	public static void cleanup() throws IOException {
		File dataFolder = new File("target/local-data");
		FileUtils.deleteDirectory(dataFolder);
		dataFolder.mkdirs();

		File configFolder = new File("target/local-config");
		FileUtils.deleteDirectory(configFolder);
		configFolder.mkdirs();

	}

	@Rule
	public GenericContainer mesh = new GenericContainer("mesh-local:latest")

			.withFileSystemBind("target/local-data", "/data", BindMode.READ_WRITE)

			.withFileSystemBind("target/local-config", "/config", BindMode.READ_WRITE)

			.withExposedPorts(8080).withLogConsumer(logConsumer);

	@Test
	public void testContainer() throws IOException {
		System.out.println(mesh.getContainerIpAddress() + ":" + mesh.getMappedPort(8080));
		System.in.read();
	}
}
