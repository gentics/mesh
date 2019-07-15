package com.gentics.mesh.image;

import java.awt.image.BufferedImage;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

@FunctionalInterface
public interface ImageAction {

	void call(String imageName, Integer width, Integer height, String color, BufferedImage refImage, String path, Flowable<Buffer> stream);

}