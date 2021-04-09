package com.gentics.mesh.core.image;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.parameter.ImageManipulationParameters;

import io.reactivex.Single;
import io.vertx.reactivex.core.buffer.Buffer;

/**
 * SPI provider interface for image manipulators.
 */
public interface ImageManipulator {

	/**
	 * Resize the given binary data and return the path to the resized file.
	 * 
	 * @param binary
	 * @param parameters
	 * @return The path to the resized file.
	 */
	Single<String> handleResize(HibBinary binary, ImageManipulationParameters parameters);
/*
	*//**
	 * Resize the given s3 binary data and return the path to the resized file.
	 *
	 * @param s3binary
	 * @param parameters
	 * @return The path to the resized file.
	 *//*
	Single<String> handleResize(Buffer s3binary, ImageManipulationParameters parameters);*/

	/**
	 * Return the cache file for the given binary and image parameters.
	 * 
	 * @param sha512sum
	 *            Hashsum of the source binary
	 * @param parameters
	 *            Resize parameters
	 * @return
	 */
	Single<CacheFileInfo> getCacheFilePath(String sha512sum, ImageManipulationParameters parameters);

	/**
	 * Read the image information from image file.
	 *
	 * @param file
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
