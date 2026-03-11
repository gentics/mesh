package com.gentics.mesh.plugin.favorite.handler;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.favorite.FavoriteService;

import io.reactivex.Completable;
import io.reactivex.Single;

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

		Single<String> typeSingle = ParameterUtil.getFromPath(context, "type", pattern);

		return typeSingle.flatMap(type -> favoriteService.getFavorite(projectName, type)).doOnSuccess(response -> {
			context.response().putHeader("Content-Type", APPLICATION_JSON);
			context.response().end(JsonUtil.toJson(response));
		}).ignoreElement();
	}

}
