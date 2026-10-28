package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.CommentService;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FetchCommentsHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public FetchCommentsHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext context) {
		// CreateCommentRequest request = JsonUtil.readValue(context.getBodyAsString(), CreateCommentRequest.class);
		String uuid = context.get("uuid");
		return commentService.fetchComment(uuid)
			.doOnEvent(jsonHandler(context))
			.ignoreElement();
	}

}
