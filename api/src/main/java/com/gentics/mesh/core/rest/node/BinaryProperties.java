package com.gentics.mesh.core.rest.node;

/**
 * POJO that is used to store binary properties of a node which holds a binary value.
 *
 */
public class BinaryProperties {

	private Integer width;
	private Integer height;
	private String sha512sum;
	private long fileSize;
	private String mimeType;
	private Integer dpi;

	public BinaryProperties() {
	}

	/**
	 * Return the image DPI.
	 * 
	 * @return Image DPI
	 */
	public Integer getDpi() {
		return dpi;
	}

	/**
	 * Set the image DPI.
	 * 
	 * @param dpi
	 *            Image DPI
	 */
	public void setDpi(Integer dpi) {
		this.dpi = dpi;
	}

	/**
	 * Return the binary filesize.
	 * 
	 * @return Filesize in bytes
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * Set the binary filesize.
	 * 
	 * @param fileSize
	 *            Filesize in bytes
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * Return the image height.
	 * 
	 * @return Image height
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Set the image height.
	 * 
	 * @param height
	 *            Image height
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	/**
	 * Return the width of the image.
	 * 
	 * @return Image width
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Set the width of the image.
	 * 
	 * @param width
	 *            Image width
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}

	/**
	 * Return the binary mimetype.
	 * 
	 * @return Binary mimetype
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Set the binary mimetype.
	 * 
	 * @param mimeType
	 *            Binary mimetype
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return Checksum
	 */
	public String getSha512sum() {
		return sha512sum;
	}

	/**
	 * Set the binary sha512 checksum.
	 * 
	 * @param sha512sum
	 *            Checksum
	 */
	public void setSha512sum(String sha512sum) {
		this.sha512sum = sha512sum;
	}

}
