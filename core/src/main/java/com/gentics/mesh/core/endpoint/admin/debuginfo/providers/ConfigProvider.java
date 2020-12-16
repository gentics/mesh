package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;

import io.reactivex.Flowable;

/**
 * 
 */
@Singleton
public class ConfigProvider implements DebugInfoProvider {
	private final DebugInfoUtil debugInfoUtil;

	@Inject
	public ConfigProvider(DebugInfoUtil debugInfoUtil) {
		this.debugInfoUtil = debugInfoUtil;
	}

	@Override
	public String name() {
		return "config";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.fromArray(
			"config/mesh.yml",
			"config/hazelcast.xml",
			"config/logback.xml",
			"config/default-distributed-db-config.json",
			"config/orientdb-server-config.xml"
		).flatMap(debugInfoUtil::readDebugInfoEntryOrEmpty);
	}
}
