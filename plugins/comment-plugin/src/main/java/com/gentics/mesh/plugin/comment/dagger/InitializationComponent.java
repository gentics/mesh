package com.gentics.mesh.plugin.comment.dagger;

import java.io.File;

import javax.inject.Singleton;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.service.TemplateService;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.reactivex.core.Vertx;

@Singleton
@Component(modules = {InitializationModule.class})
public interface InitializationComponent {
	MeshPluginService meshPluginService();

	TemplateService templateService();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(Configuration configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		Builder storageDir(@StorageDir File file);

		@BindsInstance
		Builder vertx(Vertx vertx);

		InitializationComponent build();
	}
}
