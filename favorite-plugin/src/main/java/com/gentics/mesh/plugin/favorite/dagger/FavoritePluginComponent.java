package com.gentics.mesh.plugin.favorite.dagger;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.favorite.client.AdminClient;
import com.gentics.mesh.plugin.favorite.client.UserClient;
import com.gentics.mesh.plugin.favorite.config.FavoritePluginConfig;
import com.gentics.mesh.plugin.favorite.handler.DeleteFavoriteHandler;
import com.gentics.mesh.plugin.favorite.handler.GetFavoriteHandler;
import com.gentics.mesh.plugin.favorite.handler.OverrideFavoriteHandler;
import com.gentics.mesh.plugin.favorite.handler.StoreFavoriteHandler;
import com.gentics.mesh.rest.client.MeshRestClient;
import dagger.BindsInstance;
import dagger.Component;
import io.vertx.reactivex.core.Vertx;

import javax.annotation.Nullable;
import javax.inject.Singleton;


@Singleton
@Component(modules = { FavoritePluginModule.class })
public interface FavoritePluginComponent {

	StoreFavoriteHandler storeFavoriteHandler();

	GetFavoriteHandler getFavoriteHandler();

	OverrideFavoriteHandler getOverrideFavoriteHandler();

	DeleteFavoriteHandler getDeleteFavoriteHandler();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(FavoritePluginConfig configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		Builder meshPluginService(MeshPluginService meshPluginService);

		@BindsInstance
		@Nullable
		Builder userClient(@UserClient @Nullable MeshRestClient client);

		@BindsInstance
		Builder pluginContext(PluginContext pluginContext);


		@BindsInstance
		Builder rxVertx(Vertx rxVertx);

		FavoritePluginComponent build();


	}
}
