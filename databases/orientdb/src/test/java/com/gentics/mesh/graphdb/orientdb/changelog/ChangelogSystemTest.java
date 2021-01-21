package com.gentics.mesh.graphdb.orientdb.changelog;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.gentics.mesh.changelog.Change;
import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ChangelogSystemImpl;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.maven.MavenMetadata;
import com.gentics.mesh.maven.MavenUtilities;
import com.gentics.mesh.maven.VersionNumber;
import com.gentics.mesh.metric.MetricsService;
import com.tinkerpop.blueprints.Vertex;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

@RunWith(value = Parameterized.class)
public class ChangelogSystemTest {

	File targetDir = new File("target/dump");

	private String version;

	public ChangelogSystemTest(String version) {
		this.version = version;
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
			.getMavenMetadata(new URL("https://maven.gentics.com/maven2/com/gentics/mesh/mesh-demo/maven-metadata.xml"));

		Collection<Object[]> data = new ArrayList<Object[]>();
		for (String version : metadata.getVersions()) {
			// Only test mesh release dumps since a specific version
			VersionNumber pVersion = VersionNumber.parse(version);
			if (pVersion == null) {
				continue;
			}
			if (pVersion.compareTo(VersionNumber.parse("1.4.0")) >= 0) {
				data.add(new Object[] { version });
			}
		}
		return data;
	}

	@Before
	public void downloadDump() throws IOException, ZipException {
		// TODO use released version of demo dump
		URL website = new URL(
			"https://maven.gentics.com/maven2/com/gentics/mesh/mesh-demo/" + version + "/mesh-demo-" + version + "-dump.zip");

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
	public void testChangelogSystem() throws Exception {
		OrientDBMeshOptions options = new OrientDBMeshOptions();
		options.getStorageOptions().setDirectory("target/dump/graphdb");
		options.setNodeName("dummyNode");

		Database db = getDatabase(options);
		db.setupConnectionPool();
		ChangelogSystem cls = new ChangelogSystemImpl(db, options);
		List<Change> testChanges = Arrays.asList(new ChangeDummy2(), new ChangeDummy());
		assertTrue("All changes should have been applied", cls.applyChanges(null, testChanges));
		assertTrue("All changes should have been applied", cls.applyChanges(null, testChanges));
		assertTrue("All changes should have been applied", cls.applyChanges(null, testChanges));
		Iterator<Vertex> it = db.rawTx().getVertices("name", "moped2").iterator();
		assertTrue("The changelog was executed but the expected vertex which was created could not be found.", it.hasNext());
		Vertex vertex = it.next();
		assertNotNull("The node which was created using the changelog system should be found.", vertex);
		assertFalse("The change should only be applied once but we found another moped vertex", it.hasNext());
	}

	/**
	 * Load the graph database which was configured in the mesh storage options.
	 * 
	 * @param options
	 * @return
	 */
	public static Database getDatabase(OrientDBMeshOptions options) {
		MetricsService metrics = Mockito.mock(MetricsService.class);
		Mockito.when(metrics.timer(Mockito.any())).thenReturn(Mockito.mock(Timer.class));
		Mockito.when(metrics.counter(Mockito.any())).thenReturn(Mockito.mock(Counter.class));
		Database database = new OrientDBDatabase(options, null, null, null, metrics, null, null, new OrientDBClusterManager(null, null, null, options, null), null, null, null, null, null);
		try {
			database.init(null);
			return database;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testFailingChange() {
		List<Change> listWithFailingChange = new ArrayList<>();
		listWithFailingChange.add(new ChangeDummyFailing());
		// new DaggerChangelogSpringConfiguration().build();
		// try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ChangelogSpringConfiguration.class)) {
		// ctx.start();
		// MeshOptions options = new MeshOptions();
		// options.getStorageOptions().setDirectory("target/dump/graphdb");
		// Database db = ChangelogRunner.getDatabase(options);
		// ChangelogSystem cls = new ChangelogSystem(db);
		// assertFalse("The changelog should fail", cls.applyChanges(listWithFailingChange));
		// }
	}
}
