package com.gentics.mesh.plugin.forms.handler.downloads;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;

import com.google.common.net.HttpHeaders;
import io.reactivex.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

@Singleton
public class ExportHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public ExportHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");

		return formDataService
			.export(projectName, formUuid, context, ParameterUtil.getLanguages(context))
			.flatMapCompletable(exportData -> {
				HttpServerResponse rs = context.response()
					.putHeader(HttpHeaders.CONTENT_TYPE, exportData.getFormat().mimeType)
					.putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + exportData.getDownloadFilename());

				return new io.vertx.reactivex.core.http.HttpServerResponse(rs)
					.rxSendFile(exportData.getExportFilePath())
					.doFinally(exportData.getExportFile()::delete);
			});
	}
}
