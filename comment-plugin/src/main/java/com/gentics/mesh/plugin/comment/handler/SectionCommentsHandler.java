package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.model.SectionCommentsParameters;
import com.gentics.mesh.plugin.comment.service.CommentSectionService;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SectionCommentsHandler extends AbstractCommentHandler {

	private final CommentSectionService sectionService;

	@Inject
	public SectionCommentsHandler(CommentSectionService sectionService) {
		this.sectionService = sectionService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String uuid = context.pathParam("uuid");
		SectionCommentsParameters params = SectionCommentsParameters.fromContext(context);
		return sectionService.fetchComments(uuid, params.getLanguage(), params.getOrder())
			.doOnEvent(jsonHandler(context))
			.toCompletable();
	}
}
