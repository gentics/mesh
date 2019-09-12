package com.gentics.mesh.plugin.mustread;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.test.docker.MeshDockerServer;

import io.vertx.core.Vertx;

public class MustReadPluginIT {

	@ClassRule
	public static final MeshDockerServer meshServer = new MeshDockerServer("gentics/mesh:0.40.2", Vertx.vertx())
		.withFileSystemBind("target/mesh-mustread-plugin-1.0.0.jar", "/plugins/mustread.jar")
		.waitForStartup();

	@Test
	public void testIt() throws IOException {
		for (File f : new File("target").listFiles()) {
			System.out.println(f);
		}
		System.in.read();
		fail("Fail");
	}
}
