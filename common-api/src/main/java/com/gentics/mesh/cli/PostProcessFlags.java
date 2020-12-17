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

	/**
	 * Mark the required reindex and resync flags.
	 */
	public void requireReindex() {
		setReindex(true);
		setResync(true);
	}

	/**
	 * Mark the required sync flag.
	 */
	public void requireResync() {
		setResync(true);
	}

	/**
	 * Check whether a reindex should be invoked (includes recreation of indices)
	 * 
	 * @return
	 */
	public boolean isReindex() {
		return reindex;
	}

	/**
	 * Check whether a index sync should be invoked.
	 * 
	 * @return
	 */
	public boolean isResync() {
		return resync;
	}
}
