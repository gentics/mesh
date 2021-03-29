package com.gentics.mesh.core.rest.node.field;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;

public interface S3BinaryField extends Field {

    /**
     * Uuid of the used s3binary data.
     *
     * @return
     */
    String getS3BinaryUuid();

    /**
     * Set the Uuid of the used s3binary data.
     *
     * @param uuid
     * @return
     */
    S3BinaryField setS3BinaryUuid(String uuid);

    /**
     * Return the s3binary filesize.
     *
     * @return Filesize in bytes
     */
    long getFileSize();

    /**
     * Set the s3binary filesize.
     *
     * @param fileSize
     *            Filesize in bytes
     * @return Fluent API
     */
    S3BinaryField setFileSize(long fileSize);

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
    S3BinaryField setHeight(Integer height);

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
    S3BinaryField setWidth(Integer width);

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
    S3BinaryField setMimeType(String mimeType);

    /**
     * Return the sha512 checksum.
     *
     * @return Checksum
     */
    String getSha512sum();

    /**
     * Set the s3binary sha512 checksum.
     *
     * @param sha512sum
     *            Checksum
     * @return Fluent API
     */
    S3BinaryField setSha512sum(String sha512sum);

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
    S3BinaryField setFileName(String fileName);

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
    S3BinaryField setDominantColor(String dominantColor);

    /**
     * Return the currently configured focal point.
     *
     * @return
     */
    FocalPoint getFocalPoint();

    /**
     * Set the focal point.
     *
     * @param point
     * @return
     */
    S3BinaryField setFocalPoint(FocalPoint point);

    /**
     * Set the focal point.
     *
     * @param x
     * @param y
     * @return
     */
    default S3BinaryField setFocalPoint(float x, float y) {
        setFocalPoint(new FocalPoint(x, y));
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
    S3BinaryMetadata getMetadata();

    /**
     * Set the s3binary metadata.
     *
     * @param metaData
     * @return
     */
    S3BinaryField setMetadata(S3BinaryMetadata metaData);

    @Override
    default Object getValue() {
        return getS3BinaryUuid();
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
    S3BinaryField setPlainText(String text);

}
