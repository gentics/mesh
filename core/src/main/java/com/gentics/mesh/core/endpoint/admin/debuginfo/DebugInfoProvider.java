package com.gentics.mesh.core.endpoint.admin.debuginfo;

import com.gentics.mesh.context.InternalActionContext;

import io.reactivex.Flowable;

public interface DebugInfoProvider {
	String name();
	LoadLevel loadLevel();
	Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac);
}
