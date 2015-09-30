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
	 * @return
	 */
	public Integer getDpi() {
		return dpi;
	}

	/**
	 * Set the image DPI.
	 * 
	 * @param dpi
	 */
	public void setDpi(Integer dpi) {
		this.dpi = dpi;
	}

	/**
	 * Return the binary filesize.
	 * 
	 * @return
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * Set the binary filesize.
	 * 
	 * @param fileSize
	 */
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	/**
	 * Return the image height.
	 * 
	 * @return
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Set the image height.
	 * 
	 * @param height
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	/**
	 * Return the width of the image.
	 * 
	 * @return
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Set the width of the image.
	 * 
	 * @param width
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}

	/**
	 * Return the binary mimetype.
	 * 
	 * @return
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Set the binary mimetype.
	 * 
	 * @param mimeType
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return
	 */
	public String getSha512sum() {
		return sha512sum;
	}

	/**
	 * Set the binary sha512 checksum.
	 * 
	 * @param sha512sum
	 */
	public void setSha512sum(String sha512sum) {
		this.sha512sum = sha512sum;
	}

}
