package com.gentics.mesh.core.rest.search;

public class TypeMetrics {
	private Long synced;
	private Long pending;

	public Long getSynced() {
		return synced;
	}

	public TypeMetrics setSynced(Long synced) {
		this.synced = synced;
		return this;
	}

	public Long getPending() {
		return pending;
	}

	public TypeMetrics setPending(Long pending) {
		this.pending = pending;
		return this;
	}
}
