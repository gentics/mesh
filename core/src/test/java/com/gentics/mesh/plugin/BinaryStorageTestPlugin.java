package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.binary.BinaryStoragePlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

public class BinaryStorageTestPlugin extends AbstractPlugin implements BinaryStoragePlugin {

	public BinaryStorageTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Flowable<Buffer> read(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Flowable<Buffer> read(String uuid, long start, long size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Single<Boolean> exists(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Completable store(Flowable<Buffer> stream, long size, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Completable delete(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

}
