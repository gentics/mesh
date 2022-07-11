package com.gentics.mesh.core.image.spi;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.parameter.ImageManipulationParameters;

import io.reactivex.Completable;
import io.reactivex.Single;

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
	Single<String> handleResize(Binary binary, ImageManipulationParameters parameters);

	/**
	 * Resize the given s3 binary data and return the result.
	 *
	 * @param bucketName
	 * @param cacheBucketName
	 * @param s3ObjectKey
	 * @param cacheS3ObjectKey
	 * @param filename
	 * @param parameters
	 */
	Completable handleS3CacheResize(String bucketName, String cacheBucketName, String s3ObjectKey,
			String cacheS3ObjectKey, String filename, ImageManipulationParameters parameters);

	/**
	 * Resize the given s3 binary data and return the result.
	 *
	 * @param bucketName
	 * @param s3ObjectKey
	 * @param filename
	 * @param parameters
	 */
	Single<File> handleS3Resize(String bucketName, String s3ObjectKey, String filename,
			ImageManipulationParameters parameters);

	/**
	 * Return the cache file for the given binary and image parameters.
	 *
	 * @param sha512sum  Hashsum of the source binary
	 * @param parameters Resize parameters
	 * @return
	 */

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
