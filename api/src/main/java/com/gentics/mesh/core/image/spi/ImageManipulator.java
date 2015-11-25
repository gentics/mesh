package com.gentics.mesh.core.image.spi;

import java.io.File;
import java.io.InputStream;

import com.gentics.mesh.query.impl.ImageRequestParameter;

import io.vertx.rxjava.core.buffer.Buffer;
import rx.Observable;

/**
 * SPI provider interface for image manipulators.
 */
public interface ImageManipulator {

	/**
	 * Resize the given binary file and return a buffer to the resized image data.
	 * 
	 * @param binaryFile
	 * @param sha512sum
	 * @param imageRequestParameter
	 * @return
	 */
	Observable<Buffer> handleResize(File binaryFile, String sha512sum, ImageRequestParameter imageRequestParameter);

	/**
	 * Read the inputstream and resize the image data.
	 * 
	 * @param ins
	 * @param sha512sum
	 * @param parameters
	 * @return
	 */
	Observable<Buffer> handleResize(InputStream ins, String sha512sum, ImageRequestParameter parameters);

}
