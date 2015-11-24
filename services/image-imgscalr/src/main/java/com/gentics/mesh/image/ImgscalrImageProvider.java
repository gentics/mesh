package com.gentics.mesh.image;

import java.awt.image.BufferedImage;

import org.imgscalr.Scalr;

import com.gentics.mesh.core.image.ImageProvider;

public class ImgscalrImageProvider implements ImageProvider {

	public void process(BufferedImage src, int x, int y, int cropWidth, int cropHeight, int targetWidth, int targetHeigth) {

		Scalr.crop(src, x, y, cropWidth, cropHeight);
		Scalr.resize(src, targetWidth, targetHeigth);
	}

}
