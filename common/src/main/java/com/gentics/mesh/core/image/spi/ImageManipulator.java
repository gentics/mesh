package com.gentics.mesh.core.image.spi;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.util.PropReadFileStream;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

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

	/**
	 * Return the cache file for the given sha512 checksum and image manipulation parameters.
	 * 
	 * @param sha512sum
	 * @param parameters
	 * @return
	 */
	File getCacheFile(String sha512sum, ImageManipulationParameters parameters);

	/**
	 * Read the image information from image file.
	 * 
	 * @param ins
	 * @return
	 */
	Single<ImageInfo> readImageInfo(String file);

	/**
	 * Read the image information from the image file.
	 * 
	 * @param file
	 * @return
	 * @throws Exception 
	 */
	Optional<ImageInfo> readImageInfoBlocking(String file) throws Exception;

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
