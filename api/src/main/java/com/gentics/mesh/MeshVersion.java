package com.gentics.mesh;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.util.VersionUtil;

/**
 * Provider for the Gentics Mesh version information.
 */
public interface MeshVersion {

	public static final int CURRENT_API_VERSION = 2;
	public static final String CURRENT_API_BASE_PATH = "/api/v" + MeshVersion.CURRENT_API_VERSION;

	static AtomicReference<BuildInfo> buildInfo = new AtomicReference<BuildInfo>(null);

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

	/**
	 * Return the calculated previous mesh version (patch/minor/major). If the calculation is not possible, return current version.
	 * 
	 * @return Mesh version
	 */
	static String getPreviousVersion() {
		String currentVersion = getPlainVersion();
		if (StringUtils.isBlank(currentVersion)) {
			return currentVersion;
		}
		// Remove SNAPSHOT and other suffixes
		currentVersion = currentVersion.split("-")[0];
		String[] versions = currentVersion.split("\\.");
		try {
			Path path = Path.of(MeshVersion.class.getResource("/mesh.build.properties").toURI()).getParent().resolve("../../../changelog/src/changelog/mappings").toRealPath();
			for (int i = versions.length-1; i >=0; i--) {
				int version = Integer.parseInt(versions[i]);
				int ii = i;
				if (version > 0) {
					String searchRegex = IntStream.range(0, versions.length).mapToObj(iii -> {
						if (iii < ii) {
							return versions[iii];
						} else if (iii == ii) {
							return Integer.toString(version-1);
						} else {
							return "[\\d]+";
						}
					}).collect(Collectors.joining(".", "", ".json"));
					String previousVersion = Files.walk(path)
							.map(child -> child.getFileName())
							.filter(child -> child.toString().matches(searchRegex))
							.map(child -> child.toString().split("\\.json")[0])
						.reduce("0.0.0", (old, new_) -> VersionUtil.compareVersions(old, new_) < 0 ? new_ : old, (old, new_) -> old);
					return previousVersion;
				}
			}
		} catch (Exception e) {
			System.err.println("Could not get previous Mesh version: " + currentVersion);
			e.printStackTrace();
		}
		return currentVersion;
	}
}
