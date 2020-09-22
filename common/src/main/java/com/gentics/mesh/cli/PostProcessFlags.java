package com.gentics.mesh.cli;

/**
 * Container for flags which control the startup post process actions.
 */
public class PostProcessFlags {

	boolean reindex = false;
	boolean resync = false;

	public PostProcessFlags(boolean resync, boolean reindex) {
		this.resync = resync;
		this.reindex = reindex;
	}

	public void setReindex(boolean reindex) {
		this.reindex = reindex;
	}

	public void setResync(boolean resync) {
		this.resync = resync;
	}

	public void requireReindex() {
		setReindex(true);
		setResync(true);
	}

	public void requireResync() {
		setResync(true);
	}

	public boolean isReindex() {
		return reindex;
	}

	public boolean isResync() {
		return resync;
	}
}
