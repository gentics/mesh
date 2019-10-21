package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import io.reactivex.Flowable;

@Singleton
public class PluginsProvider implements DebugInfoProvider {
	private final MeshPluginManager pluginManager;

	@Inject
	public PluginsProvider(MeshPluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	@Override
	public String name() {
		return "plugins";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		List<PluginResponse> plugins = pluginManager.getStartedMeshPlugins().stream()
			.map(MeshPlugin::toResponse)
			.collect(Collectors.toList());

		return Flowable.just(DebugInfoBufferEntry.asJson("plugins.json", plugins));
	}
}
