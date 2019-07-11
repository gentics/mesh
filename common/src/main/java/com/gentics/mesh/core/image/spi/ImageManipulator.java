package com.gentics.mesh.core.image.spi;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.util.PropReadFileStream;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * SPI provider interface for image manipulators.
 */
public interface ImageManipulator {

	/**
	 * Resize the given binary data and return a buffer to the resized image data.
	 *
	 * @param stream
	 *            Binary data stream to be used for resizing
	 * @param cacheKey
	 *            Key used to name the local cache file
	 * @param imageRequestParameter
	 * @return
	 */
	Single<PropReadFileStream> handleResize(Flowable<Buffer> stream, String cacheKey, ImageManipulationParameters imageRequestParameter);

	Single<String> handleResize(Binary binary, ImageManipulationParameters parameters);

	/**
	 * Return the cache file for the given sha512 checksum and image manipulation parameters.
	 *
	 * The provided <code>sha512sum</code> will be used to determine the cache directory, and the image manipulation parameters determine the
	 * filename without the extension.
	 *
	 * When multiple files match the given hashsum and parameters, only the first found file will be returned.
	 *
	 * This method either returns an existing file, or a file object which filename is the name of the cache file without the extension.
	 *
	 * @deprecated Use {@link #getCacheFilePath(String, ImageManipulationParameters)} instead.
	 *
	 * @param sha512sum
	 * @param parameters
	 * @return
	 *            A <code>File</code> object referencing an existing file, when the file already exists in the cache, or a <code>File</code> object
	 *            which filename is to be used as base filename without the file extension
	 */
	@Deprecated
	File getCacheFile(String sha512sum, ImageManipulationParameters parameters);

	Single<CacheFileInfo> getCacheFilePath(String sha512sum, ImageManipulationParameters parameters);

	/**
	 * Read the image information from image file.
	 *
	 * @param ins
	 * @return
	 */
	Single<ImageInfo> readImageInfo(String file);

	/**
	 * Return the dominant color in the image.
	 *
	 * @param image
	 * @return
	 */
	int[] calculateDominantColor(BufferedImage image);

	/**
	 * Extract the metadata from the image data stream.
	 *
	 * @param ins
	 * @return
	 */
	Single<Map<String, String>> getMetadata(InputStream ins);
}
