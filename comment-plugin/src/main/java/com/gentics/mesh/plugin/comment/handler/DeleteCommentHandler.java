package com.gentics.mesh.plugin.comment.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.CommentService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class DeleteCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public DeleteCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext context) {
		Single<String> singleUuid = ParameterUtil.getUuidFromPath(context, "uuid");
		return singleUuid.flatMapCompletable(uuid -> commentService.deleteComment(uuid))
			.doOnEvent(emptyResponseHandler(context));
	}
}
