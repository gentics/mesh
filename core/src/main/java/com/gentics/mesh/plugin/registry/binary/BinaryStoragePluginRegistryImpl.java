package com.gentics.mesh.plugin.registry.binary;

import javax.inject.Singleton;

import com.gentics.mesh.plugin.MeshPlugin;

import io.reactivex.Completable;

@Singleton
public class BinaryStoragePluginRegistryImpl implements BinaryStoragePluginRegistry {

	@Override
	public Completable register(MeshPlugin plugin) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkForConflict(MeshPlugin plugin) {
		// TODO Auto-generated method stub
		
	}

}
