package com.gentics.mesh.core.rest.node;

public class BinaryProperties {

	private Integer width;
	private Integer height;
	private String sha512sum;
	private long fileSize;
	private String mimeType;
	private Integer dpi;

	public BinaryProperties() {
	}

	public Integer getDpi() {
		return dpi;
	}

	public void setDpi(Integer dpi) {
		this.dpi = dpi;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getSha512sum() {
		return sha512sum;
	}

	public void setSha512sum(String sha512sum) {
		this.sha512sum = sha512sum;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}
}
