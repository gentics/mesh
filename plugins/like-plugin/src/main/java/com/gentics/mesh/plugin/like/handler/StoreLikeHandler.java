package com.gentics.mesh.plugin.like.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.like.service.LikeService;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;

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
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "nodeUuid");
		Single<String> langSingle = ParameterUtil.getFromPath(context, "lang", ParameterUtil.LANGUAGE_PATTERN);

		return Single
				.zip(uuidSingle, langSingle,
						(uuid, lang) -> likeService.postLike(projectName, uuid, lang, context.body().asString()))
				.flatMap(x -> x).doOnEvent((response, t) -> {
					if (t == null) {
						context.response().setStatusCode(HttpResponseStatus.CREATED.code()).end();
					}
				}).ignoreElement();
	}

}
