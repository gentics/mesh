package com.gentics.mesh.core.endpoint.admin.debuginfo;

import com.gentics.mesh.context.InternalActionContext;

import io.reactivex.Flowable;

public interface DebugInfoProvider {
	String name();
	Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac);
}
