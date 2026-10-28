package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.model.SectionCommentsParameters;
import com.gentics.mesh.plugin.comment.service.CommentSectionService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Completable;
import io.reactivex.Single;

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
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "uuid");
		SectionCommentsParameters params = SectionCommentsParameters.fromContext(context);
		return uuidSingle.flatMap(uuid -> sectionService.fetchComments(uuid, params.getLanguage(), params.getOrder()))
			.doOnEvent(jsonHandler(context))
			.ignoreElement();
	}
}
