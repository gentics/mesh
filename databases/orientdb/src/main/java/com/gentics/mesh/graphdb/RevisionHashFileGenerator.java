package com.gentics.mesh.graphdb;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/***
 * Generator which will produce the revision hash file which can later be used to construct a list of revision hashes per release.
 */
public class RevisionHashFileGenerator {

	public static void main(String[] args) throws IOException {
		System.out.println("Generating revision hash...");
		String hash = new OrientDBDatabase(null, null, null, null, null, null, null, null, null, null).getDatabaseRevision();
		System.out.println("Hash: " + hash);
		File file = new File("target", "database-revision.txt");
		FileUtils.writeStringToFile(file, hash);
		System.out.println("Wrote file {" + file.getAbsolutePath() + "}");
	}
	
}
