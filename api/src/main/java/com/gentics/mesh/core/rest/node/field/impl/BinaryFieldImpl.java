package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BinaryField;

public class BinaryFieldImpl implements BinaryField {

	@JsonIgnore
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
	public BinaryField setDpi(Integer dpi) {
		this.dpi = dpi;
		return this;
	}

	@Override
	public long getFileSize() {
		return fileSize;
	}

	@Override
	public BinaryField setFileSize(long fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	@Override
	public Integer getHeight() {
		return height;
	}

	@Override
	public BinaryField setHeight(Integer height) {
		this.height = height;
		return this;
	}

	@Override
	public Integer getWidth() {
		return width;
	}

	@Override
	public BinaryField setWidth(Integer width) {
		this.width = width;
		return this;
	}

	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public BinaryField setMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	@Override
	public String getSha512sum() {
		return sha512sum;
	}

	@Override
	public BinaryField setSha512sum(String sha512sum) {
		this.sha512sum = sha512sum;
		return this;
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public BinaryField setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}
}
