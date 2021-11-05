package com.gentics.mesh.handler;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.context.InternalActionContext;

/**
 * Util methods, related to the content versioning.
 *
 * @author plyhun
 *
 */
public final class VersionUtils {

	private VersionUtils() {}
	
	/**
	 * Creates the start of a route for specific version. Example: /api/v2
	 * 
	 * @param version
	 * @return
	 */
	public static String baseRoute(int version) {
		return "/api/v" + version;
	}

	/**
	 * A stream that generates all available baseRoutes. Example: ["/api/v1", "/api/v2"]
	 * 
	 * @return
	 */
	public static Stream<String> generateVersionMountpoints() {
		return IntStream.rangeClosed(1, MeshVersion.CURRENT_API_VERSION)
			.mapToObj(VersionUtils::baseRoute);
	}

	/**
	 * Return the basepath for the given action context.
	 * 
	 * @param ac
	 * @return API Basepath
	 */
	public static String baseRoute(InternalActionContext ac) {
		return baseRoute(ac.getApiVersion());
	}
}
