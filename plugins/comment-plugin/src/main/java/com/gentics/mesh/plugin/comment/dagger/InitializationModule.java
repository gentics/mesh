package com.gentics.mesh.plugin.comment.dagger;

import javax.inject.Singleton;

import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.Module;
import dagger.Provides;
import io.vertx.reactivex.core.Vertx;

import java.io.File;

@Module
public class InitializationModule {
	@Singleton
	@Provides
	static MeshPluginService provideMeshPluginService(Vertx vertx, @AdminClient MeshRestClient adminClient, @StorageDir File storageDir) {
		return new MeshPluginService(vertx, adminClient, storageDir);
	}
}
