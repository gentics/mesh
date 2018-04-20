package com.gentics.mesh.core.data.search.bulk;

/**
 * A bulk entry is a container which holds needed information to construct an entry for an Elasticsearch bulk update request.
 */
public interface BulkEntry {

	enum Action {
		CREATE, DELETE, INDEX, UPDATE;

		public String id() {
			return name().toLowerCase();
		}
	};

	/**
	 * Transform the entry information into the string representation needed for the bulk update.
	 * 
	 * @return
	 */
	String toBulkString();

	/**
	 * Returns the action of the entry.
	 * 
	 * @return
	 */
	Action getBulkAction();

	String getIndexName();

	String getDocumentId();
}
