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
		
		if(!type.matches("[a-zA-Z0-9]*")) {
			throw new InvalidTypeError(type);
		}
		
		return favoriteService.postFavorite(projectName, type, context.getBodyAsString()).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
			}
		}).toCompletable();
	}

}
