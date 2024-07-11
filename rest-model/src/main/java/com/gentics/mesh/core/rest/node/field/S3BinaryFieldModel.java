package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.node.field.image.FocalPointModel;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataModel;

/**
 * Interface for the s3 binary field
 */
public interface S3BinaryFieldModel extends FieldModel {

    /**
     * Uuid of the used s3binary data.
     *
     * @return
     */
    String getS3binaryUuid();

    /**
     * Set the Uuid of the used s3binary data.
     *
     * @param uuid
     * @return
     */
    S3BinaryFieldModel setS3binaryUuid(String uuid);

    /**
     * Return the s3binary filesize.
     *
     * @return Filesize in bytes
     */
    Long getFileSize();

    /**
     * Set the s3binary filesize.
     *
     * @param fileSize
     *            Filesize in bytes
     * @return Fluent API
     */
    S3BinaryFieldModel setFileSize(Long fileSize);

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
     * @return Fluent API
     */
    S3BinaryFieldModel setHeight(Integer height);

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
     * @return Fluent API
     */
    S3BinaryFieldModel setWidth(Integer width);

    /**
     * Return the s3binary mimetype.
     *
     * @return S3Binary mimetype
     */
    String getMimeType();

    /**
     * Set the binary mimetype.
     *
     * @param mimeType
     *            Binary mimetype
     * @return Fluent API
     */
    S3BinaryFieldModel setMimeType(String mimeType);

    /**
     * Return the s3binary filename of the node (may be null when no s3binary value was set)
     *
     * @return Filename
     */
    String getFileName();

    /**
     * Set the s3binary filename.
     *
     * @param fileName
     *            Filename
     * @return Fluent API
     */
    S3BinaryFieldModel setFileName(String fileName);

    /**
     * Return the dominant color of the image.
     *
     * @return
     */
    String getDominantColor();

    /**
     * Set the dominant color of the image.
     *
     * @param dominantColor
     * @return
     */
    S3BinaryFieldModel setDominantColor(String dominantColor);

    /**
     * Return the currently configured focal point.
     *
     * @return
     */
    FocalPointModel getFocalPoint();

    /**
     * Set the focal point.
     *
     * @param point
     * @return
     */
    S3BinaryFieldModel setFocalPoint(FocalPointModel point);

    /**
     * Set the focal point.
     *
     * @param x
     * @param y
     * @return
     */
    default S3BinaryFieldModel setFocalPoint(float x, float y) {
        setFocalPoint(new FocalPointModel(x, y));
        return this;
    }

    /**
     * Check whether contains any values.
     *
     * @return
     */
    @JsonIgnore
    boolean hasValues();

    /**
     * Return the s3binary metadata.
     *
     * @return
     */
    S3BinaryMetadataModel getMetadata();

    /**
     * Set the s3binary metadata.
     *
     * @param metaData
     * @return
     */
    S3BinaryFieldModel setMetadata(S3BinaryMetadataModel metaData);

    @Override
    default Object getValue() {
        return getS3binaryUuid();
    }

    /**
     * Returns the plain text that was extracted from the uploaded document.
     *
     * @return
     */
    String getPlainText();

    /**
     * Set the plain text from the binary document.
     *
     * @return
     */
    S3BinaryFieldModel setPlainText(String text);

    /**
     * Returns the S3 Object Key which is the reference to AWS.
     *
     * @return
     */
    String getS3ObjectKey();

    /**
     * Set the S3 Object Key.
     *
     * @return
     */
    S3BinaryFieldModel setS3ObjectKey(String objectKey);

}
