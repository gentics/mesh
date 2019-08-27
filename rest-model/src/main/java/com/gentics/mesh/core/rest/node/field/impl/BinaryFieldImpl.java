package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;

public class BinaryFieldImpl implements BinaryField {

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.BINARY.toString();
	}

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the binary data. Two fields which share the same binary data will also share the same Uuid.")
	private String binaryUuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("File name of the binary data. This information can also be use to locate the node via the webroot API. The segment field must be set accordingly.")
	private String fileName;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Width of the image.")
	private Integer width;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Height of the image.")
	private Integer height;

	@JsonProperty(required = true)
	@JsonPropertyDescription("SHA 512 checksum of the file.")
	private String sha512sum;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Size of the file in bytes.")
	private long fileSize;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Determined mimetype of the file.")
	private String mimeType;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The dominant color of the image. This information can be used to set the background color of the container div for an image achieve an pinterest styled gallery.")
	private String dominantColor;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The focal point of the image. The point can be used in combination with the focal point cropping in order to keep the focused area in the center of the cropped image.")
	private FocalPoint focalPoint;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Metadata of the upload. This object may contain exif data of images or meta data from PDF files.")
	private BinaryMetadata metadata;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Plain text content of the upload. This can be the text content of a word or PDF document.")
	private String plainText;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Extra ID for storage providers.")
	private String storageId;

	@Override
	public String getBinaryUuid() {
		return binaryUuid;
	}

	@Override
	public BinaryField setBinaryUuid(String uuid) {
		this.binaryUuid = uuid;
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

	@Override
	public String getDominantColor() {
		return dominantColor;
	}

	@Override
	public BinaryField setDominantColor(String dominantColor) {
		this.dominantColor = dominantColor;
		return this;
	}

	@Override
	public FocalPoint getFocalPoint() {
		return focalPoint;
	}

	@Override
	public BinaryField setFocalPoint(FocalPoint point) {
		this.focalPoint = point;
		return this;
	}

	@Override
	public BinaryMetadata getMetadata() {
		return metadata;
	}

	@Override
	public BinaryField setMetadata(BinaryMetadata metadata) {
		this.metadata = metadata;
		return this;
	}
	
	@Override
	public String getPlainText() {
		return plainText;
	}
	
	@Override
	public BinaryField setPlainText(String text) {
		this.plainText = text;
		return this;
	}

	@Override
	public String getStorageId() {
		return storageId;
	}

	@Override
	public BinaryField setStorageId(String id) {
		this.storageId = id;
		return this;
	}

	@Override
	@JsonIgnore
	public boolean hasValues() {
		return getDominantColor() != null || getFileName() != null && getMimeType() != null || getFocalPoint() != null || getMetadata() != null || getSha512sum() != null || getStorageId() != null;
	}
}
