package com.gentics.mesh.core.rest.schema;

/**
 * Determines what parts of the binary data should be extracted and sent to Elasticsearch.
 */
public class BinaryExtractOptions {
	private boolean content;
	private boolean metadata;

	public BinaryExtractOptions() {
	}

	public BinaryExtractOptions(boolean content, boolean metadata) {
		this.content = content;
		this.metadata = metadata;
	}

	/**
	 * If true, text content (e.g. in PDFs, word documents, etc.) will be extracted from the binary data.
	 * @return
	 */
	public boolean getContent() {
		return content;
	}

	/**
	 * If true, text content (e.g. in PDFs, word documents, etc.) will be extracted from the binary data.
	 * @param content
	 * @return
	 */
	public BinaryExtractOptions setContent(boolean content) {
		this.content = content;
		return this;
	}

	/**
	 * If true, metadata (e.g. JPEG exif) will be extracted from the binary data.
	 * @return
	 */
	public boolean getMetadata() {
		return metadata;
	}

	/**
	 * If true, metadata (e.g. JPEG exif) will be extracted from the binary data.
	 * @param metadata
	 * @return
	 */
	public BinaryExtractOptions setMetadata(boolean metadata) {
		this.metadata = metadata;
		return this;
	}
}
