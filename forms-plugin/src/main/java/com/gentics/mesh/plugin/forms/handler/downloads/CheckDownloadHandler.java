package com.gentics.mesh.plugin.forms.handler.downloads;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.FormParameterUtil;
import io.reactivex.Completable;
import io.reactivex.Single;

import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * Check if the requested download is ready and if so return the download URL.
 *
 * <p>
 *     The response will also contain information if another download is
 *     currently being prepared.
 * </p>
 */
public class CheckDownloadHandler extends AbstractFormsHandler {
	public final static Pattern TYPE_PATTERN = Pattern.compile("\\w+", Pattern.CASE_INSENSITIVE);

	private final FormDataService formDataService;
	private final FormsConfig configuration;

	@Inject
	public CheckDownloadHandler(FormDataService formDataService, FormsConfig configuration) {
		this.formDataService = formDataService;
		this.configuration = configuration;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> downloadTypeSingle = ParameterUtil.getFromPath(context, "downloadType", TYPE_PATTERN);
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single.zip(formUuidSingle, downloadTypeSingle, languagesSingle, (formUuid, downloadType, languages) -> {
			String options = "export".equals(downloadType) ? FormParameterUtil.getExportOptions(context, configuration)
					: "";

			return formDataService.checkDownload(projectName, formUuid, options, languages,
					downloadType);
		}).flatMap(x -> x).doOnEvent(HandlerUtil.jsonObjectHandler(context, false)).ignoreElement();
	}
}
