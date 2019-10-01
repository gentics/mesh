package com.gentics.mesh.plugin.like.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.like.service.LikeService;

import io.reactivex.Completable;

@Singleton
public class DeleteLikeHandler extends AbstractLikeHandler {
	private final LikeService likeService;

	@Inject
	public DeleteLikeHandler(LikeService likeService) {
		this.likeService = likeService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("nodeUuid");
		String lang = context.pathParam("lang");

		return likeService.deleteLike(projectName, uuid, lang).doOnSuccess(response -> {
			context.response().setStatusCode(204).end();
		}).toCompletable();
	}

}
