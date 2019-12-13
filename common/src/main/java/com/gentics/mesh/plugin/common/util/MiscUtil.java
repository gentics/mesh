package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Miscellaneous utility methods for Mesh plugins.
 */
public class MiscUtil {

	/**
	 * Load the specified resource as a String.
	 *
	 * <p>
	 *     Note that this method will wrap {@link IOException IOExceptions}
	 *     into {@link RuntimeException RuntimeExceptions}.
	 * </p>
	 *
	 * @param path The path to the resource to load
	 * @return The string contents of the specified resource
	 */
	public static String loadResource(String path) {
		try {
			return IOUtils.toString(
				MeshPluginService.class.getResourceAsStream(path),
				Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
