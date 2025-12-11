package com.gentics.mesh.plugin.like.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.like.service.LikeService;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;

@Singleton
public class StoreLikeHandler extends AbstractLikeHandler {
	private final LikeService likeService;

	@Inject
	public StoreLikeHandler(LikeService likeService) {
		this.likeService = likeService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("nodeUuid");
		String lang = context.pathParam("lang");
		return likeService.postLike(projectName, uuid, lang, context.body().asString()).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
			}
		}).ignoreElement();
	}

}
