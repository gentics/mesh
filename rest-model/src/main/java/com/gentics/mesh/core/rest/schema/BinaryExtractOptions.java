package com.gentics.mesh.core.rest.schema;

/**
 * Determines what parts of the binary data should be extracted and sent to Elasticsearch.
 */
public class BinaryExtractOptions {
	private Boolean content;
	private Boolean metadata;

	public BinaryExtractOptions(Boolean content, Boolean metadata) {
		this.content = content;
		this.metadata = metadata;
	}

	/**
	 * If true, text content (e.g. in PDFs, word documents, etc.) will be extracted from the binary data.
	 * @return
	 */
	public Boolean getContent() {
		return content;
	}

	/**
	 * If true, text content (e.g. in PDFs, word documents, etc.) will be extracted from the binary data.
	 * @param content
	 * @return
	 */
	public BinaryExtractOptions setContent(Boolean content) {
		this.content = content;
		return this;
	}

	/**
	 * If true, metadata (e.g. JPEG exif) will be extracted from the binary data.
	 * @return
	 */
	public Boolean getMetadata() {
		return metadata;
	}

	/**
	 * If true, metadata (e.g. JPEG exif) will be extracted from the binary data.
	 * @param metadata
	 * @return
	 */
	public BinaryExtractOptions setMetadata(Boolean metadata) {
		this.metadata = metadata;
		return this;
	}
}
