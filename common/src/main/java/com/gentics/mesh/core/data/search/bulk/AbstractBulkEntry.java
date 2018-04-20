package com.gentics.mesh.core.data.search.bulk;

public abstract class AbstractBulkEntry implements BulkEntry {

	private final String indexName;

	private final String documentId;

	public AbstractBulkEntry(String indexName, String documentId) {
		this.indexName = indexName;
		this.documentId = documentId;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getDocumentId() {
		return documentId;
	}

}
