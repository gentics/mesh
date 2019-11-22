package com.gentics.mesh.plugin.favorite.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.favorite.FavoriteService;
import com.gentics.mesh.plugin.favorite.error.InvalidTypeError;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;

@Singleton
public class StoreFavoriteHandler extends AbstractFavoriteHandler{
	private final FavoriteService favoriteService;

	@Inject
	public StoreFavoriteHandler(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String type = context.pathParam("type");
		String favoriteID = context.pathParam("id");
		if(!type.matches(regexMatch)) {

			throw new InvalidTypeError(type);
		}

		return favoriteService.postFavorite(projectName, type, favoriteID).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
			}
		}).ignoreElement();
	}

}
