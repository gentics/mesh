package com.gentics.mesh.plugin.favorite.dagger;

import java.util.Map;

import javax.inject.Singleton;

import com.gentics.mesh.plugin.favorite.SchemaInitializer;
import com.gentics.mesh.plugin.favorite.client.AdminClient;
import com.gentics.mesh.plugin.favorite.config.FavoritePluginConfig;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.reactivex.core.Vertx;

@Singleton
@Component
public interface InitializationComponent {
	SchemaInitializer schemaInitializer();


	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(FavoritePluginConfig configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);
		
		@BindsInstance
		Builder rootNodeUuids(@RootNodeUuids Map<String, String> rootNodeUuids);

		@BindsInstance
		Builder vertx(Vertx vertx);

		InitializationComponent build();
	}
}