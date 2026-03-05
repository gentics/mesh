package com.gentics.mesh.plugin.forms.handler.downloads;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.google.common.net.HttpHeaders;

import io.reactivex.Completable;
import io.reactivex.Single;
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
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single
				.zip(formUuidSingle, languagesSingle,
						(formUuid, languages) -> formDataService.export(projectName, formUuid, context, languages))
				.flatMap(x -> x).flatMapCompletable(exportData -> {
					HttpServerResponse rs = context.response()
							.putHeader(HttpHeaders.CONTENT_TYPE, exportData.getFormat().mimeType)
							.putHeader(HttpHeaders.CONTENT_DISPOSITION,
									"attachment; filename=" + exportData.getDownloadFilename());

					return new io.vertx.reactivex.core.http.HttpServerResponse(rs)
							.rxSendFile(exportData.getExportFilePath()).doFinally(exportData.getExportFile()::delete);
				});
	}
}
