package com.gentics.mesh.changelog;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.Vertex;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class TestChangelogRunner {

	File targetDir = new File("target/dump");

	@Before
	public void downloadDump() throws IOException, ZipException {

		FileUtils.deleteDirectory(targetDir);
		URL website = new URL(
				"http://artifactory.office/repository/lan.snapshots.mesh/com/gentics/mesh/mesh-demo/0.6.3-SNAPSHOT/mesh-demo-0.6.3-20160318.101338-6-dump.zip");
		ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		File zipFile = new File("target" + File.separator + "dump.zip");
		FileOutputStream fos = new FileOutputStream(zipFile);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

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
			Iterator<Vertex> it = db.rawTx().getVertices("name", "moped").iterator();
			Vertex vertex = it.next();
			assertNotNull("The node which was created using the changelog system should be found.", vertex);
			assertFalse("The change should only be applied once but we found another moped vertex", it.hasNext());
		}
	}
}
