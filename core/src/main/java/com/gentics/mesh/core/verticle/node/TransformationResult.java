package com.gentics.mesh.core.verticle.node;

import com.gentics.mesh.core.image.spi.ImageInfo;

public class TransformationResult {

	private String hash;
	private long size;
	private ImageInfo info;

	public TransformationResult(String hash, long size, ImageInfo info) {
		this.hash = hash;
		this.size = size;
		this.info = info;
	}

	public String getHash() {
		return hash;
	}

	public ImageInfo getInfo() {
		return info;
	}

	public long getSize() {
		return size;
	}

}
