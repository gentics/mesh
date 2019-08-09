package com.gentics.mesh.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.registry.PluginRegistry;

import io.reactivex.Completable;

@Singleton
public class AuthServicePluginRegistry implements PluginRegistry {

	private Map<String, AuthServicePlugin> plugins = new HashMap<>();

	@Inject
	public AuthServicePluginRegistry() {
	}

	@Override
	public Completable register(MeshPlugin plugin) {
		if (plugin instanceof AuthServicePlugin) {
			plugins.put(plugin.id(), (AuthServicePlugin) plugin);
		}
		return Completable.complete();
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		if (plugin instanceof AuthServicePlugin) {
			plugins.remove(plugin.id());
		}
		return Completable.complete();
	}

	@Override
	public void checkForConflict(MeshPlugin plugin) {
		// Not needed
	}

	public List<AuthServicePlugin> getPlugins() {
		return plugins.values().stream().collect(Collectors.toList());
	}

}
