package com.gentics.mesh.core.rest.schema;

/**
 * Determines what parts of the s3binary data should be extracted and sent to Elasticsearch.
 */
public class S3BinaryExtractOptions {
	private boolean content;
	private boolean metadata;

	/**
	 * If true, text content (e.g. in PDFs, word documents, etc.) will be extracted from the s3binary data.
	 * @return
	 */
	public boolean getContent() {
		return content;
	}

	/**
	 * If true, text content (e.g. in PDFs, word documents, etc.) will be extracted from the s3binary data.
	 * @param content
	 * @return
	 */
	public S3BinaryExtractOptions setContent(boolean content) {
		this.content = content;
		return this;
	}

	/**
	 * If true, metadata (e.g. JPEG exif) will be extracted from the s3binary data.
	 * @return
	 */
	public boolean getMetadata() {
		return metadata;
	}

	/**
	 * If true, metadata (e.g. JPEG exif) will be extracted from the s3binary data.
	 * @param metadata
	 * @return
	 */
	public S3BinaryExtractOptions setMetadata(boolean metadata) {
		this.metadata = metadata;
		return this;
	}
}
