package com.gentics.mesh.changelog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.changelog.changes.Change_424FA7436B6541269E6CE90C8C3D812D;
import com.gentics.mesh.changelog.changes.Change_424FA7436B6541269E6CE90C8C3D812D3;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.Vertex;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

@RunWith(value = Parameterized.class)
public class TestChangelogRunner {

	File targetDir = new File("target/dump");

	private String version;

	public TestChangelogRunner(String version) {
		this.version = version;
	}

	@BeforeClass
	public static void setupOnce() {
		// Add dummy changes
		ChangesList.getList().add(new Change_424FA7436B6541269E6CE90C8C3D812D());
		ChangesList.getList().add(new Change_424FA7436B6541269E6CE90C8C3D812D3());
	}

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 * @throws Exception
	 */
	@Parameters(name = "{index}: version {0}")
	public static Collection<Object[]> data() throws Exception {

		MavenMetadata metadata = MavenUtilities
				.getMavenMetadata(new URL("http://artifactory.office/repository/lan.releases/com/gentics/mesh/mesh-demo/maven-metadata.xml"));

		Collection<Object[]> data = new ArrayList<Object[]>();
		for (String version : metadata.getVersions()) {
			// Only test mesh release dumps since a specific version
			if (VersionNumber.parse(version).compareTo(VersionNumber.parse("0.6.1")) >= 0) {
				data.add(new Object[] { version });
			}
		}
		return data;
	}

	@Before
	public void downloadDump() throws IOException, ZipException {
		// TODO use released version of demo dump
		URL website = new URL(
				"http://artifactory.office/repository/lan.snapshots.mesh/com/gentics/mesh/mesh-demo/0.6.3-SNAPSHOT/mesh-demo-0.6.3-20160318.101338-6-dump.zip");

		FileUtils.deleteDirectory(targetDir);

		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		File zipFile = new File("target" + File.separator + "dump.zip");
		FileOutputStream fos = new FileOutputStream(zipFile);
		try {
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} finally {
			fos.close();
		}
		ZipFile zip = new ZipFile(zipFile);
		zip.extractAll(targetDir.getAbsolutePath());
		zipFile.delete();
	}

	@Test
	public void testRunner() throws Exception {
		MeshOptions options = new MeshOptions();
		options.getStorageOptions().setDirectory("target/dump/graphdb");
		new ChangelogRunner().run(options);
	}

	@Test
	public void testChangelogSystem() {
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ChangelogSpringConfiguration.class)) {
			ctx.start();
			MeshOptions options = new MeshOptions();
			options.getStorageOptions().setDirectory("target/dump/graphdb");
			Database db = ChangelogRunner.getDatabase(options);
			ChangelogSystem cls = new ChangelogSystem(db);
			assertTrue("All changes should have been applied", cls.applyChanges());
			assertTrue("All changes should have been applied", cls.applyChanges());
			assertTrue("All changes should have been applied", cls.applyChanges());
			Iterator<Vertex> it = db.rawTx().getVertices("name", "moped2").iterator();
			Vertex vertex = it.next();
			assertNotNull("The node which was created using the changelog system should be found.", vertex);
			assertFalse("The change should only be applied once but we found another moped vertex", it.hasNext());
		}
	}
}
