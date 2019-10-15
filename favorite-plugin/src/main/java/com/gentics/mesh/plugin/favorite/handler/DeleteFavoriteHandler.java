package com.gentics.mesh.plugin.favorite.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.favorite.FavoriteService;
import com.gentics.mesh.plugin.favorite.model.PostedFavorite;

import io.reactivex.Completable;

@Singleton
public class DeleteFavoriteHandler extends AbstractFavoriteHandler {
	private final FavoriteService favoriteService;

	@Inject
	public DeleteFavoriteHandler(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String type = context.pathParam("type");
		
		PostedFavorite posted = JsonUtil.readValue(context.getBodyAsString(), PostedFavorite.class);
		String value = posted.getValue();
		
		return favoriteService.deleteFavorite(projectName, type, value).doOnSuccess(response -> {
			context.response().setStatusCode(204).end();
		}).ignoreElement();
	}

}
