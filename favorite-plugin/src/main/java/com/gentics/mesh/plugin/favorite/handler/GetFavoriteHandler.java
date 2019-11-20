package com.gentics.mesh.plugin.favorite.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.favorite.FavoriteService;
import com.gentics.mesh.plugin.favorite.error.InvalidTypeError;

import io.reactivex.Completable;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;

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
		
		if(!type.matches("[A-Za-z0-9\\-\\_]+")) {
			throw new InvalidTypeError(type);
		}

		return favoriteService.getFavorite(projectName, type).doOnSuccess(response -> {
			context.response().putHeader("Content-Type", APPLICATION_JSON);
			context.response().end(JsonUtil.toJson(response));
		}).ignoreElement();
	}

}
