package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.IOUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.maven.MavenMetadata;
import com.gentics.mesh.maven.MavenUtilities;
import com.gentics.mesh.maven.VersionNumber;

/**
 * The generator will iterate over all released Gentics Mesh versions and generate a table which contains all database revision hashes.
 */
public class DatabaseRevisionTableGenerator extends AbstractRenderingGenerator {

	private static final String BASE_PATH = "https://maven.gentics.com/maven2/com/gentics/mesh/mesh-orientdb/";

	private static final String DB_REV_TABLE_TEMPLATE_NAME = "db-revs-table.hbs";;

	public DatabaseRevisionTableGenerator(File outputFolder) throws IOException {
		super(new File(outputFolder, "tables"), false);
	}

	public DatabaseRevisionTableGenerator(File file, boolean cleanDir) throws IOException {
		super(file, cleanDir);
	}

	public static void main(String[] args) throws Exception {
		new DatabaseRevisionTableGenerator(new File("target", "output"), true).run();
	}

	public void run() throws MalformedURLException, Exception {
		MavenMetadata metadata = MavenUtilities
			.getMavenMetadata(new URL(BASE_PATH + "maven-metadata.xml"));

		List<Map<String, String>> entries = new ArrayList<>();
		for (String version : metadata.getVersions()) {
			// 0.16.1 introduced the revision artifact
			System.out.println("Checking: " + version);
			VersionNumber parsedVersion = VersionNumber.parse(version);
			if (parsedVersion != null && parsedVersion.compareTo(VersionNumber.parse("0.16.1")) >= 0) {
				URL revisionFileUrl = new URL(BASE_PATH + version + "/" + "mesh-orientdb-" + version + "-revision.txt");
				String hash = IOUtils.readStringFromStream(revisionFileUrl.openStream());
				System.out.println("Found version {" + version + "} with hash {" + hash + "}");
				Map<String, String> map = new HashMap<>();
				map.put("version", version);
				map.put("revision", hash);
				entries.add(map);
			}
		}
		// Add the local version as well.
		Map<String, String> local = new HashMap<>();
		String version = Mesh.getPlainVersion();
		if (!version.endsWith("-SNAPSHOT")) {
			local.put("version", version);
			local.put("revision", new OrientDBDatabase(null, null, null, null, null, null, null).getDatabaseRevision());
			entries.add(local);
		}

		Map<String, Object> context = new HashMap<>();
		context.put("entries", entries);
		String table = renderTable(context, getTemplate(DB_REV_TABLE_TEMPLATE_NAME));
		writeFile("mesh-db-revs.adoc-include", table);
	}

}
