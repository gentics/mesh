package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

	/**
	 * Split the given byte array into chunks with the specified maximum size.
	 *
	 * @param input The input array to be split
	 * @param maxSize The maximum size of a chunk
	 * @return A list with the resized chunks
	 */
	public static List<byte[]> splitBytes(byte[] input, int maxSize) {
		return IntStream.range(0, (int) Math.ceil((double) input.length / maxSize))
			.map(i -> i * maxSize)
			.mapToObj(i -> Arrays.copyOfRange(input, i, i + Math.min(maxSize, input.length - i)))
			.collect(Collectors.toList());
	}
}
