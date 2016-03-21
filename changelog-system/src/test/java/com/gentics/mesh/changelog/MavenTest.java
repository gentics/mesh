package com.gentics.mesh.changelog;

import java.net.URL;

import org.junit.Test;

public class MavenTest {

	@Test
	public void testName() throws Exception {
		MavenMetadata data = MavenUtilities
				.getMavenMetadata(new URL("http://artifactory.office/repository/lan.releases/com/gentics/mesh/mesh-demo/maven-metadata.xml"));
		System.out.println(data.getVersion());
		for (String version : data.getVersions()) {
			System.out.println(version);
		}
		//		MavenUtilities.getMavenMetadata("junit", "junit");
	}
}
