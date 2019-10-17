package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;

import io.reactivex.Flowable;

public class BinaryDiskUsageEndpoint implements DebugInfoProvider {
	@Override
	public String name() {
		return "binaryDiskUsage";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.MEDIUM;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return null;
	}
}
