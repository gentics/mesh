package com.gentics.mesh.cli;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.AbstractIntegrationTest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MeshIntegerationTest extends AbstractIntegrationTest {

	public final static long TIMEOUT = DEFAULT_TIMEOUT_SECONDS * 20;

	@Before
	public void cleanup() throws IOException {
		new File("mesh.yml").delete();
		FileUtils.deleteDirectory(new File("data"));
		FileUtils.deleteDirectory(new File("config"));
	}

	@Test
	public void testStartup() throws Exception {
		Mesh mesh = Mesh.create();
		mesh.rxRun().blockingAwait(TIMEOUT, TimeUnit.SECONDS);
		mesh.shutdown();
	}

	@Test
	public void testStartupWithOptions() throws Exception {
		MeshOptions options = new MeshOptions();
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		options.getSearchOptions().setUrl(null);
		options.setNodeName("TestNode");

		Mesh mesh = Mesh.create(options);
		mesh.rxRun().blockingAwait(TIMEOUT, TimeUnit.SECONDS);
		mesh.shutdown();
	}
}
