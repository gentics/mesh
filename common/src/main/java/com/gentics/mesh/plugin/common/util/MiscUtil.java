package com.gentics.mesh.plugin.common.util;

import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
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
	 * @deprecated Use {@link #loadResourceString(String)} instead
	 * @param path The path to the resource to load
	 * @return The string contents of the specified resource
	 */
	@Deprecated
	public static String loadResource(String path) {
		return loadResourceString(path);
	}

	/**
	 * Load the specified resource as a String.
	 *
	 * @see #loadResourceStream(String)
	 * @param path The path to the resource to load
	 * @return The string contents of the specified resource
	 */
	public static String loadResourceString(String path) {
		try (InputStream stream = loadResourceStream(path)) {
			return IOUtils.toString(stream, Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Could not load resource \"" + path + "\": " + e.getMessage(), e);
		}
	}

	/**
	 * Load the specified resource as a byte array.
	 *
	 * @see #loadResourceStream(String)
	 * @param path The path to the resource to load
	 * @return The contents of the specified resource as a byte array
	 */
	public static byte[] loadResourceBytes(String path) {
		try (InputStream stream = loadResourceStream(path)) {
			return IOUtils.toByteArray(stream);
		} catch (IOException e) {
			throw new RuntimeException("Could not load resource \"" + path + "\": " + e.getMessage(), e);
		}
	}


	/**
	 * Load the specified resource as a byte array.
	 *
	 * @see #loadResourceStream(String)
	 * @param path The path to the resource to load
	 * @return The contents of the specified resource as a byte array
	 */
	@Nonnull
	public static InputStream loadResourceStream(String path) throws FileNotFoundException {
		InputStream stream = MiscUtil.class.getResourceAsStream(path);

		if (stream == null) {
			throw new FileNotFoundException(path);
		}

		return stream;
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

	/**
	 * Get the first line of the given string
	 * @param string string
	 * @return first line
	 */
	public static String getFirstLine(String string) {
		if (StringUtils.isBlank(string)) {
			return string;
		}
		int newlineIndex = StringUtils.indexOfAny(string, '\r', '\n');
		if (newlineIndex >= 0) {
			return StringUtils.substring(string, 0, newlineIndex);
		} else {
			return string;
		}
	}
}
