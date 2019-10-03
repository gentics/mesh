package com.gentics.mesh.plugin.formgenerator.handler;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormDataService;

import io.reactivex.Completable;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;

@Singleton
public class UpdatePostedJsonDataHandler extends AbstractFormGeneratorHandler {
	private final FormDataService formDataService;

	@Inject
	public UpdatePostedJsonDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String formDataUuid = context.pathParam("formDataUuid");
		MultiMap postedData = context.request().formAttributes();
		String referer = context.request().getHeader("Referer");

		Set<FileUpload> fileUploads = new HashSet<>(context.fileUploads());
		// remove empty uploads
		fileUploads.removeIf(up -> up.size() == 0);

		return formDataService.update(projectName, formUuid, formDataUuid, postedData, fileUploads, referer, context, false).doOnSuccess(response -> {
			context.response().end(JsonUtil.toJson(response));
		}).ignoreElement();
	}
}
