package com.gentics.mesh.plugin.favorite.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.favorite.FavoriteService;

import io.reactivex.Completable;
import io.reactivex.Single;

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
		Single<String> typeSingle = ParameterUtil.getFromPath(context, "type", pattern);
		Single<String> favoriteIDSingle = ParameterUtil.getFromPath(context, "id", idPattern);

		return Single
				.zip(typeSingle, favoriteIDSingle,
						(type, favoriteID) -> favoriteService.deleteFavorite(projectName, type, favoriteID))
				.flatMap(x -> x).doOnSuccess(response -> {
					context.response().setStatusCode(204).end();
				}).ignoreElement();
	}

}
