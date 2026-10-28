package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.CommentService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Completable;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FetchCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public FetchCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext context) {
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "uuid");
		return uuidSingle.flatMap(uuid -> commentService.fetchComment(uuid, true))
			.doOnEvent(jsonHandler(context))
			.ignoreElement();
	}

}
