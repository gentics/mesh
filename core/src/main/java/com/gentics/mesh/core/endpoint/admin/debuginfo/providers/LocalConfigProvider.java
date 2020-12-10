package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;

import io.reactivex.Flowable;

@Singleton
public class LocalConfigProvider implements DebugInfoProvider {

	private final LocalConfigApi localConfigApi;

	@Inject
	public LocalConfigProvider(LocalConfigApi localConfigApi) {
		this.localConfigApi = localConfigApi;
	}

	@Override
	public String name() {
		return "localConfig";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return localConfigApi.getActiveConfig()
			.map(localConfig -> DebugInfoBufferEntry.asJson("localConfig.json", localConfig))
			.toFlowable();
	}
}
