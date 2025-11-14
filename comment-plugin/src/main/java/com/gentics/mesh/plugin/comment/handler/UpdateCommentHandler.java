package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;
import com.gentics.mesh.plugin.comment.service.CommentService;
import io.reactivex.Completable;

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
		String commentUuid = context.pathParam("uuid");
		UpdateCommentRequest request = JsonUtil.readValue(context.body().asString(), UpdateCommentRequest.class);
		return commentService.updateComment(commentUuid, request)
			.doOnEvent(jsonHandler(context))
			.ignoreElement();
	}
}
