package com.gentics.mesh.core.endpoint.admin.debuginfo;

import io.reactivex.Flowable;

public interface DebugInfoProvider {
	String name();
	Flowable<DebugInfoEntry> debugInfoEntries();
}
