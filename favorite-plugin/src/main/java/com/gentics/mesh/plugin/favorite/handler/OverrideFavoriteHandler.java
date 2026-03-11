package com.gentics.mesh.plugin.favorite.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.favorite.FavoriteService;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;

import io.reactivex.Single;

@Singleton
public class OverrideFavoriteHandler extends AbstractFavoriteHandler {
	private final FavoriteService favoriteService;

	@Inject
	public OverrideFavoriteHandler(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> typeSingle = ParameterUtil.getFromPath(context, "type", pattern);
		return typeSingle.flatMap(type -> favoriteService.overrideFavorite(projectName, type, context.body().asJsonObject()))
				.doOnEvent((response, t) -> {
					if (t == null) {
						context.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
					}
				}).ignoreElement();
	}

}
