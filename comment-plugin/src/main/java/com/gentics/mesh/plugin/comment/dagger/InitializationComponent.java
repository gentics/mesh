package com.gentics.mesh.plugin.comment.dagger;

import com.gentics.mesh.plugin.comment.SchemaInitializer;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.service.TemplateService;
import com.gentics.mesh.rest.client.MeshRestClient;
import dagger.BindsInstance;
import dagger.Component;
import io.vertx.reactivex.core.Vertx;

import javax.inject.Singleton;
import java.io.File;

@Singleton
@Component
public interface InitializationComponent {
	SchemaInitializer schemaInitializer();

	TemplateService templateService();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(Configuration configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		Builder storageDir(File file);

		@BindsInstance
		Builder vertx(Vertx vertx);

		InitializationComponent build();
	}
}
