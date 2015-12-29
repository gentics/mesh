package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BinaryField;

public class BinaryFieldImpl implements BinaryField {

	@Override
	public String getType() {
		return FieldTypes.BINARY.toString();
	}

	private String fileName;
	private Integer width;
	private Integer height;
	private String sha512sum;
	private long fileSize;
	private String mimeType;
	private Integer dpi;

	@Override
	public Integer getDpi() {
		return dpi;
	}

	@Override
	public void setDpi(Integer dpi) {
		this.dpi = dpi;
	}

	@Override
	public long getFileSize() {
		return fileSize;
	}

	@Override
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	@Override
	public Integer getHeight() {
		return height;
	}

	@Override
	public void setHeight(Integer height) {
		this.height = height;
	}

	@Override
	public Integer getWidth() {
		return width;
	}

	@Override
	public void setWidth(Integer width) {
		this.width = width;
	}

	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String getSha512sum() {
		return sha512sum;
	}

	@Override
	public void setSha512sum(String sha512sum) {
		this.sha512sum = sha512sum;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
