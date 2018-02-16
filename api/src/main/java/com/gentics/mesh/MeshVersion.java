package com.gentics.mesh;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provider for the Gentics Mesh version information.
 */
public interface MeshVersion {

	static AtomicReference<BuildInfo> buildInfo = new AtomicReference<>(null);

	/**
	 * Return the mesh build information.
	 * 
	 * @return Mesh version and build timestamp.
	 */
	static BuildInfo getBuildInfo() {
		try {
			if (buildInfo.get() == null) {
				Properties buildProperties = new Properties();
				buildProperties.load(Mesh.class.getResourceAsStream("/mesh.build.properties"));
				// Cache the build information
				buildInfo.set(new BuildInfo(buildProperties));
			}
			return buildInfo.get();
		} catch (Exception e) {
			return new BuildInfo("unknown", "unknown");
		}
		// Package pack = MeshImpl.class.getPackage();
		// return pack.getImplementationVersion();
	}

	/**
	 * Return the mesh version (without build timestamp)
	 *
	 * @return Mesh version
	 */
	static String getPlainVersion() {
		return getBuildInfo().getVersion();
	}

}
