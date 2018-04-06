package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.core.image.spi.ImageInfo;

/**
 * Image transformation result object.
 */
public class TransformationResult {

	private String sha512sum;
	private long size;
	private ImageInfo imageInfo;
	private String filePath;

	/**
	 * Create a new result.
	 * 
	 * @param sha512sum
	 *            New sha512 checksum of the image
	 * @param size
	 *            New image binary size
	 * @param imageInfo
	 *            New image properties (width, height..)
	 * @param filePath
	 *            Path to the image cache file
	 */
	public TransformationResult(String sha512sum, long size, ImageInfo imageInfo, String filePath) {
		this.sha512sum = sha512sum;
		this.size = size;
		this.imageInfo = imageInfo;
		this.filePath = filePath;
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

	/**
	 * Return the image cache file path.
	 * 
	 * @return
	 */
	public String getFilePath() {
		return filePath;
	}

}
