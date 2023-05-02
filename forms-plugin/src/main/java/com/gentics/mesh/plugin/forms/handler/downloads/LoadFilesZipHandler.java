package com.gentics.mesh.plugin.forms.handler.downloads;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import com.google.common.net.HttpHeaders;
import io.reactivex.Completable;
import io.vertx.reactivex.core.http.HttpServerResponse;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
		String uuid = context.pathParam("formUuid");

		return formDataService.zipBinaries(projectName, uuid, ParameterUtil.getLanguages(context))
			.flatMapCompletable(formAndFile -> {
				context.response().putHeader(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(uuid, formAndFile.getLeft().getName(), "zip"));
				return new HttpServerResponse(context.response()).rxSendFile(formAndFile.getRight().getAbsolutePath()).doFinally(formAndFile.getRight()::delete);
			});
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
