package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.core.image.spi.ImageInfo;

import io.vertx.core.http.impl.MimeMapping;

/**
 * Image transformation result object.
 */
public class TransformationResult {

	private String sha512sum;
	private long size;
	private ImageInfo imageInfo;
	private String filePath;
	private String mimeType;

	/**
	 * Create a new result.
	 *
	 * The {@link #mimeType} is automatically derived from the given <code>filePath</code>.
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
		this(
			sha512sum,
			size,
			imageInfo,
			filePath,
			MimeMapping.getMimeTypeForFilename(filePath));
	}

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
	 * @param mimeType
	 *            The mimeType of the resulting file
	 */
	public TransformationResult(String sha512sum, long size, ImageInfo imageInfo, String filePath, String mimeType) {
		this.sha512sum = sha512sum;
		this.size = size;
		this.imageInfo = imageInfo;
		this.filePath = filePath;
		this.mimeType = mimeType;
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

	/**
	 * Return the MIME type of the image.
	 *
	 * @return
	 */
	public String getMimeType() {
		return mimeType;
	}
}
