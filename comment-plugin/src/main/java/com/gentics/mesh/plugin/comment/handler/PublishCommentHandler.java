package com.gentics.mesh.plugin.comment.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.CommentService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class PublishCommentHandler extends AbstractCommentHandler {

	private final CommentService commentService;

	@Inject
	public PublishCommentHandler(CommentService commentService) {
		this.commentService = commentService;
	}

	@Override
	public Completable handle(PluginContext context) {
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "uuid");
		return uuidSingle.flatMapCompletable(uuid -> commentService.publishComment(uuid));
	}

}
