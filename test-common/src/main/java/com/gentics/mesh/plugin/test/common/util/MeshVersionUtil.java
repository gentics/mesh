package com.gentics.mesh.plugin.test.common.util;

import java.net.HttpURLConnection;
import java.net.URL;

import com.gentics.mesh.MeshVersions;

/**
 * Utility class which helps with selection and handling of Gentics Mesh versions and docker image locations.
 */
public final class MeshVersionUtil {

	private static final String LTS_IMAGE = "docker.apa-it.at/gentics/mesh";
	private static final String OSS_IMAGE = "gentics/mesh";

	private MeshVersionUtil() {

	}

	/**
	 * Check whether the provided version is an OSS release.
	 * 
	 * @param version
	 * @return
	 */
	public static boolean isOSS(String version) {
		try {
			URL url = new URL("https://maven.gentics.com/maven2/com/gentics/mesh/mesh-server/" + version + "/");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();
			int code = connection.getResponseCode();
			return code == 200;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the image which matches to the local client API version.
	 * 
	 * @return
	 */
	public static String getMatchingImage() {
		String version = MeshVersions.getPlainVersion();
		if (isOSS(version)) {
			return OSS_IMAGE + ":" + version;
		} else {
			return LTS_IMAGE + ":" + version;
		}
	}

}
