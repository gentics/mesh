package com.gentics.mesh.core.endpoint.admin.debuginfo;

import com.gentics.mesh.context.InternalActionContext;

import io.reactivex.Flowable;

/**
 * Provider for a specific type of debug information.
 */
public interface DebugInfoProvider {

	/**
	 * Name of the provider.
	 * 
	 * @return
	 */
	String name();

	/**
	 * Handler which processes the provider.
	 * 
	 * @param ac
	 * @return
	 */
	Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac);
}
