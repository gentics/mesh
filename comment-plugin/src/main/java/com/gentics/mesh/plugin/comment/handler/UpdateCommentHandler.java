package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;
import com.gentics.mesh.plugin.comment.service.CommentService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Completable;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UpdateCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public UpdateCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext context) {
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "uuid");
		UpdateCommentRequest request = JsonUtil.readValue(context.body().asString(), UpdateCommentRequest.class);
		return uuidSingle.flatMap(commentUuid -> commentService.updateComment(commentUuid, request))
			.doOnEvent(jsonHandler(context))
			.ignoreElement();
	}
}
