package com.gentics.mesh.plugin.forms.handler.downloads;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.google.common.net.HttpHeaders;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.http.HttpServerResponse;

@Singleton
public class LoadFilesZipHandler extends AbstractFormsHandler {

	private final FormDataService formDataService;

	@Inject
	public LoadFilesZipHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single.zip(uuidSingle, languagesSingle, (uuid, languages) -> formDataService
				.zipBinaries(projectName, uuid, languages).flatMapCompletable(formAndFile -> {
					context.response().putHeader(HttpHeaders.CONTENT_DISPOSITION,
							getContentDisposition(uuid, formAndFile.getLeft().getName(), "zip"));
					return new HttpServerResponse(context.response())
							.rxSendFile(formAndFile.getRight().getAbsolutePath())
							.doFinally(formAndFile.getRight()::delete);
				})).flatMapCompletable(x -> x);
	}

	/**
	 * Get the value for the <code>Content-Dispostion</code> HTTP header.
	 *
	 * The method constructs a filename from the form name and the current date and time.
	 *
	 * @param formName
	 *            The name of the form
	 * @param extension
	 *            The file extension to be used (if it does not start with a '.', it will be prepended)
	 *
	 * @return A Content-Disposition value with a filename made up from the form name and the current date
	 */
	protected String getContentDisposition(String uuid, String formName, String extension) {
		if (!extension.startsWith(".")) {
			extension = "." + extension;
		}

		return "attachment; filename=forms-files_"
			+ (formName == null || formName.isEmpty() ? uuid : formName)
			+ "_"
			+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"))
			+ extension;
	}
}
