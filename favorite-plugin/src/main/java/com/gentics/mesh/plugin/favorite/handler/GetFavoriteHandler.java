package com.gentics.mesh.plugin.favorite.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.favorite.FavoriteService;
import com.gentics.mesh.plugin.favorite.error.InvalidTypeError;

import io.reactivex.Completable;

@Singleton
public class GetFavoriteHandler extends AbstractFavoriteHandler {
	private final FavoriteService favoriteService;

	@Inject
	public GetFavoriteHandler(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String type = context.pathParam("type");
		
		if(!type.matches("[a-zA-Z0-9]*")) {
			throw new InvalidTypeError(type);
		}

		return favoriteService.getFavorite(projectName, type).doOnSuccess(response -> {
			context.response().end(JsonUtil.toJson(response));
		}).ignoreElement();
	}

}
