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
public class StoreFavoriteHandler extends AbstractFavoriteHandler{
	private final FavoriteService favoriteService;

	@Inject
	public StoreFavoriteHandler(FavoriteService favoriteService) {
		this.favoriteService = favoriteService;
	}

	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> typeSingle = ParameterUtil.getFromPath(context, "type", pattern);
		Single<String> favoriteIDSingle = ParameterUtil.getFromPath(context, "id", idPattern);

		return Single
				.zip(typeSingle, favoriteIDSingle,
						(type, favoriteID) -> favoriteService.postFavorite(projectName, type, favoriteID))
				.flatMap(x -> x).doOnEvent((response, t) -> {
					if (t == null) {
						context.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
					}
				}).ignoreElement();
	}

}
