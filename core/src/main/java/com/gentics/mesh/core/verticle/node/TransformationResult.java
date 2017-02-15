package com.gentics.mesh.core.verticle.node;

import com.gentics.mesh.core.image.spi.ImageInfo;

public class TransformationResult {

	private String sha512sum;
	private long size;
	private ImageInfo imageInfo;

	public TransformationResult(String sha512sum, long size, ImageInfo imageInfo) {
		this.sha512sum = sha512sum;
		this.size = size;
		this.imageInfo = imageInfo;
	}

	public String getHash() {
		return sha512sum;
	}

	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	public long getSize() {
		return size;
	}

}
