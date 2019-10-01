package com.gentics.mesh.plugin.like.handler;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.like.service.LikeService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpServerResponse;

@Singleton
public class GetLikeHandler extends AbstractLikeHandler {
	private final LikeService likeService;

	@Inject
	public GetLikeHandler(LikeService likeService) {
		this.likeService = likeService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("nodeUuid");
		String lang = context.pathParam("lang");

		return likeService.getLike(projectName, uuid, lang).doOnSuccess(model-> {
			HttpServerResponse resp = context.response();
			resp.putHeader(CONTENT_TYPE, APPLICATION_JSON);
			resp.end(JsonUtil.toJson(model));
		}).toCompletable();
	}

}
