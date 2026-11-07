package com.gentics.mesh.plugin.like.dagger;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.like.SchemaInitializer;
import com.gentics.mesh.plugin.like.config.LikePluginConfig;
import com.gentics.mesh.plugin.like.handler.DeleteLikeHandler;
import com.gentics.mesh.plugin.like.handler.GetLikeHandler;
import com.gentics.mesh.plugin.like.handler.GetStatisticsHandler;
import com.gentics.mesh.plugin.like.handler.StoreLikeHandler;
import com.gentics.mesh.plugin.like.handler.TemplateEngineHandler;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.Vertx;

@Singleton
@Component(modules = {LikePluginModule.class})
public interface LikePluginComponent {
	SchemaInitializer schemaInitializer();

	StoreLikeHandler storeLikeHandler();

	GetLikeHandler getLikeHandler();

	DeleteLikeHandler deleteLikeHandler();

	GetStatisticsHandler getStatisticsHandler();

	TemplateEngineHandler templateEngineHandler();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(LikePluginConfig configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		@Nullable
		Builder userClient(@UserClient @Nullable MeshRestClient client);

		@BindsInstance
		Builder rootNodeUuids(@RootNodeUuids Map<String, String> rootNodeUuids);

		@BindsInstance
		Builder vertx(Vertx vertx);

		LikePluginComponent build();
	}
}
