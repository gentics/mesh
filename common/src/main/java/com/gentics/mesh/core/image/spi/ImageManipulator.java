package com.gentics.mesh.core.image.spi;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import com.gentics.mesh.parameter.ImageManipulationParameters;

import io.vertx.core.buffer.Buffer;
import rx.Single;
import rx.functions.Func0;

/**
 * SPI provider interface for image manipulators.
 */
public interface ImageManipulator {

	/**
	 * Resize the given binary file and return a buffer to the resized image data.
	 * 
	 * @param binaryFile
	 *            Binary file in the filesystem to be used for resizing
	 * @param sha512sum
	 * @param imageRequestParameter
	 * @return
	 */
	Single<Buffer> handleResize(File binaryFile, String sha512sum, ImageManipulationParameters imageRequestParameter);

	/**
	 * Read the {@link InputStream} and resize the image data.
	 * 
	 * @param ins
	 * @param sha512sum
	 * @param parameters
	 * @return Buffer which contains the resized image data
	 */
	Single<Buffer> handleResize(InputStream ins, String sha512sum, ImageManipulationParameters parameters);

	/**
	 * Return the cache file for the given sha512 checksum and image manipulation parameters.
	 * 
	 * @param sha512sum
	 * @param parameters
	 * @return
	 */
	File getCacheFile(String sha512sum, ImageManipulationParameters parameters);

	/**
	 * Read the image information from the given image data stream.
	 * 
	 * @param insFunc
	 * @return
	 */
	Single<ImageInfo> readImageInfo(Func0<InputStream> insFunc);

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
