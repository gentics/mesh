package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.gentics.mesh.plugin.comment.service.CommentService;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CreateCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public CreateCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext event) {
		CreateCommentRequest request = JsonUtil.readValue(event.body().asString(), CreateCommentRequest.class);
		return commentService.createComment(request)
			.doOnEvent(jsonHandler(event))
			.ignoreElement();
	}
}
