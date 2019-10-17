package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Flowable;

@Singleton
public class ActiveConfigProvider implements DebugInfoProvider {
	private final MeshOptions meshOptions;

	@Inject
	public ActiveConfigProvider(MeshOptions meshOptions) {
		this.meshOptions = meshOptions;
	}

	@Override
	public String name() {
		return "activeConfig";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.LIGHT;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return Flowable.just(DebugInfoBufferEntry.fromString("activeConfig.json", JsonUtil.toJson(meshOptions)));
	}
}
