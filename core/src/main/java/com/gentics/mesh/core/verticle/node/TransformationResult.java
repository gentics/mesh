package com.gentics.mesh.core.verticle.node;

import com.gentics.mesh.core.image.spi.ImageInfo;

/**
 * Image transformation result object.
 */
public class TransformationResult {

	private String sha512sum;
	private long size;
	private ImageInfo imageInfo;

	/**
	 * Create a new result.
	 * 
	 * @param sha512sum
	 *            New sha512 checksum of the image
	 * @param size
	 *            New image binary size
	 * @param imageInfo
	 *            New image properties (width, height..)
	 */
	public TransformationResult(String sha512sum, long size, ImageInfo imageInfo) {
		this.sha512sum = sha512sum;
		this.size = size;
		this.imageInfo = imageInfo;
	}

	/**
	 * Return the hash of the image.
	 * 
	 * @return
	 */
	public String getHash() {
		return sha512sum;
	}

	/**
	 * Return the image properties of the file.
	 * 
	 * @return
	 */
	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	/**
	 * Return the size of the image in bytes.
	 * 
	 * @return
	 */
	public long getSize() {
		return size;
	}

}
