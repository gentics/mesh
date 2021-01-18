package com.gentics.mesh.image;

import java.awt.image.BufferedImage;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * Action for a image operation.
 */
@FunctionalInterface
public interface ImageAction {

	/**
	 * Run the image operation.
	 * 
	 * @param imageName
	 * @param width
	 * @param height
	 * @param color
	 * @param refImage
	 * @param path
	 * @param stream
	 */
	void call(String imageName, Integer width, Integer height, String color, BufferedImage refImage, String path, Flowable<Buffer> stream);

}