package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.CommentService;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeleteCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public DeleteCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String uuid = context.pathParam("uuid");
		return commentService.deleteComment(uuid)
			.doOnEvent(emptyResponseHandler(context));
	}
}
