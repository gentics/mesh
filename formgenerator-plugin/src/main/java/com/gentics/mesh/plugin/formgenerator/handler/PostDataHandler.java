package com.gentics.mesh.plugin.formgenerator.handler;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormDataService;

import io.reactivex.Completable;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;

@Singleton
public class PostDataHandler extends AbstractFormGeneratorHandler {
	private final FormDataService formDataService;

	@Inject
	public PostDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");
		MultiMap postedData = context.request().formAttributes();
		String referer = context.request().getHeader("Referer");

		Set<FileUpload> fileUploads = new HashSet<>(context.fileUploads());
		// remove empty uploads
		fileUploads.removeIf(up -> up.size() == 0);

		return formDataService.create(projectName, uuid, postedData, fileUploads, referer, context, true).doOnSuccess(response -> {
			String redirectUrl = response.getRedirectUrl();
			if (redirectUrl != null && !"".equals(redirectUrl)) {
				context.response().setStatusCode(303).putHeader("Location", redirectUrl).end();
			} else {
				context.response().setStatusCode(205).end();
			}
		}).ignoreElement();
	}
}
