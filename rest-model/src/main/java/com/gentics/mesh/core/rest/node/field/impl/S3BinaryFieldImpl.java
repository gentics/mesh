package com.gentics.mesh.core.rest.node.field.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;

/**
 * @see S3BinaryField
 */
public class S3BinaryFieldImpl implements S3BinaryField {

    @JsonIgnore
    @Override
    public String getType() {
        return FieldTypes.S3BINARY.toString();
    }

    @JsonProperty(required = true)
    @JsonPropertyDescription("Uuid of the s3 binary data. Two fields which share the same s3 binary data will also share the same Uuid.")
    private String s3binaryUuid;

    @JsonProperty(required = false)
    @JsonPropertyDescription("File name of the s3 binary data. This information can also be use to locate the node via the webroot API. The segment field must be set accordingly.")
    private String fileName;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Width of the image.")
    private Integer width;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Height of the image.")
    private Integer height;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Size of the file in bytes.")
    private Long fileSize;

    @JsonProperty(required = false)
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
    private S3BinaryMetadata metadata;

    @JsonProperty(required = false)
    @JsonPropertyDescription("Plain text content of the upload. This can be the text content of a word or PDF document.")
    private String plainText;

    @JsonProperty(required = false)
    @JsonPropertyDescription("S3 object key. Serves as reference to AWS.")
    private String s3ObjectKey;

    @Override
    public String getS3binaryUuid() {
        return s3binaryUuid;
    }

    @Override
    public S3BinaryField setS3binaryUuid(String uuid) {
        this.s3binaryUuid = uuid;
        return this;
    }

    @Override
    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    @Override
    public S3BinaryField setS3ObjectKey(String s3ObjectKey) {
        this.s3ObjectKey = s3ObjectKey;
        return this;
    }

    @Override
    public Long getFileSize() {
        return fileSize;
    }

    @Override
    public S3BinaryField setFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    @Override
    public Integer getHeight() {
        return height;
    }

    @Override
    public S3BinaryField setHeight(Integer height) {
        this.height = height;
        return this;
    }

    @Override
    public Integer getWidth() {
        return width;
    }

    @Override
    public S3BinaryField setWidth(Integer width) {
        this.width = width;
        return this;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public S3BinaryField setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public S3BinaryField setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    @Override
    public String getDominantColor() {
        return dominantColor;
    }

    @Override
    public S3BinaryField setDominantColor(String dominantColor) {
        this.dominantColor = dominantColor;
        return this;
    }

    @Override
    public FocalPoint getFocalPoint() {
        return focalPoint;
    }

    @Override
    public S3BinaryField setFocalPoint(FocalPoint point) {
        this.focalPoint = point;
        return this;
    }

    @Override
    public S3BinaryMetadata getMetadata() {
        return metadata;
    }

    @Override
    public S3BinaryField setMetadata(S3BinaryMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @Override
    public String getPlainText() {
        return plainText;
    }

    @Override
    public S3BinaryField setPlainText(String text) {
        this.plainText = text;
        return this;
    }

    @Override
    @JsonIgnore
    public boolean hasValues() {
        return getS3ObjectKey() != null || getDominantColor() != null || getFileName() != null && getMimeType() != null || getFocalPoint() != null || getMetadata() != null || getS3ObjectKey() != null;
    }
}