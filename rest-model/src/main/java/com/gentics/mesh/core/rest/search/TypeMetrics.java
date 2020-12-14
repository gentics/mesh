package com.gentics.mesh.core.rest.search;

/**
 * Elasticsearch index type metrics.
 */
public class TypeMetrics {

	private Long synced;
	private Long pending;

	/**
	 * Return amount of already synced documents.
	 * 
	 * @return
	 */
	public Long getSynced() {
		return synced;
	}

	/**
	 * Set the amount of synced documents.
	 * 
	 * @param synced
	 * @return
	 */
	public TypeMetrics setSynced(Long synced) {
		this.synced = synced;
		return this;
	}

	/**
	 * Return the amount of pending documents.
	 * 
	 * @return
	 */
	public Long getPending() {
		return pending;
	}

	/**
	 * Set the amount of pending documents.
	 * 
	 * @param pending
	 * @return
	 */
	public TypeMetrics setPending(Long pending) {
		this.pending = pending;
		return this;
	}
}
