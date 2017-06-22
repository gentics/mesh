package com.gentics.mesh.distributed;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

public class TestContainerTest {

	@Rule
	public GenericContainer mesh = new GenericContainer("mesh-local:latest").withFileSystemBind("target/local-data", "/data", BindMode.READ_WRITE)
			.withExposedPorts(8080);

	@Test
	public void testContainer() throws IOException {
		System.out.println(mesh.getContainerIpAddress() + ":" + mesh.getMappedPort(8080));
		System.in.read();
	}
}
