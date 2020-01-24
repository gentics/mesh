package com.gentics.mesh.plugin.common.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.google.common.base.Charsets;

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
			InputStream stream = MeshPluginService.class.getResourceAsStream(path);
			if (stream == null) {
				throw new FileNotFoundException(path);
			}
			String resource = IOUtils.toString(
				stream,
				Charsets.UTF_8);
			stream.close();
			return resource;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
