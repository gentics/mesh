package com.gentics.mesh.plugin.registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.binary.BinaryStoragePlugin;

import io.reactivex.Completable;

@Singleton
public class BinaryStoragePluginRegistry implements PluginRegistry {

	private Map<String, BinaryStoragePlugin> plugins = new HashMap<>();

	@Inject
	public BinaryStoragePluginRegistry() {
	}

	@Override
	public Completable register(MeshPlugin plugin) {
		if (plugin instanceof BinaryStoragePlugin) {
			plugins.put(plugin.id(), (BinaryStoragePlugin) plugin);
		}
		return Completable.complete();
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		if (plugin instanceof BinaryStoragePlugin) {
			plugins.remove(plugin.id());
		}
		return Completable.complete();
	}

	@Override
	public void checkForConflict(MeshPlugin plugin) {
		// Not needed
	}

	public List<BinaryStoragePlugin> getPlugins() {
		return plugins.values().stream().collect(Collectors.toList());
	}

}
