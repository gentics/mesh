package com.gentics.mesh.plugin.comment.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.CommentService;

import io.reactivex.Completable;

@Singleton
public class UnpublishCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public UnpublishCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}
	
	@Override
	public Completable handle(PluginContext context) {
		String uuid = context.pathParam("uuid");
		return commentService.unpublishComment(uuid);
	}

}
