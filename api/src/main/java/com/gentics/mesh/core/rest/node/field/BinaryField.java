package com.gentics.mesh.core.rest.node.field;

/**
 * A binary field is a field which can store binary and image related meta data.
 */
public interface BinaryField extends Field {

	/**
	 * Return the image DPI.
	 * 
	 * @return Image DPI
	 */
	Integer getDpi();

	/**
	 * Set the image DPI.
	 * 
	 * @param dpi
	 *            Image DPI
	 */
	void setDpi(Integer dpi);

	/**
	 * Return the binary filesize.
	 * 
	 * @return Filesize in bytes
	 */
	long getFileSize();

	/**
	 * Set the binary filesize.
	 * 
	 * @param fileSize
	 *            Filesize in bytes
	 */
	void setFileSize(long fileSize);

	/**
	 * Return the image height.
	 * 
	 * @return Image height
	 */
	Integer getHeight();

	/**
	 * Set the image height.
	 * 
	 * @param height
	 *            Image height
	 */
	void setHeight(Integer height);

	/**
	 * Return the width of the image.
	 * 
	 * @return Image width
	 */
	Integer getWidth();

	/**
	 * Set the width of the image.
	 * 
	 * @param width
	 *            Image width
	 */
	void setWidth(Integer width);

	/**
	 * Return the binary mimetype.
	 * 
	 * @return Binary mimetype
	 */
	String getMimeType();

	/**
	 * Set the binary mimetype.
	 * 
	 * @param mimeType
	 *            Binary mimetype
	 */
	void setMimeType(String mimeType);

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return Checksum
	 */
	String getSha512sum();

	/**
	 * Set the binary sha512 checksum.
	 * 
	 * @param sha512sum
	 *            Checksum
	 */
	void setSha512sum(String sha512sum);

	/**
	 * Return the binary filename of the node (may be null when no binary value was set)
	 * 
	 * @return Filename
	 */
	String getFileName();

	/**
	 * Set the binary filename
	 * 
	 * @param fileName
	 *            Filename
	 */
	void setFileName(String fileName);

}
