package com.gentics.mesh.plugin.forms.handler;

import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.rest.client.MeshBinaryResponse;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.User;

@Singleton
public class LoadFormDataMultiBinaryItemHandler extends AbstractFormsHandler {
	public final static Pattern BINARY_FIELD_NAME_PATTERN = Pattern.compile("\\w+", Pattern.CASE_INSENSITIVE);
	public final static Pattern INDEX_FIELD_NAME_PATTERN = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);

	private static final Logger log = LoggerFactory.getLogger(LoadFormDataMultiBinaryItemHandler.class);

	private final FormDataService formDataService;

	@Inject
	public LoadFormDataMultiBinaryItemHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		User user = context.user();

		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> formDataUuidSingle = ParameterUtil.getUuidFromPath(context, "formDataUuid");
		Single<String> binaryFieldNameSingle = ParameterUtil.getFromPath(context, "binaryFieldName", BINARY_FIELD_NAME_PATTERN);
		Single<Integer> multiBinaryIndexSingle = ParameterUtil.getFromPath(context, "index", INDEX_FIELD_NAME_PATTERN).flatMap(index -> {
			try {
				return Single.just(Integer.parseInt(index));
			} catch (Exception e) {
				log.error("Could not parse multi binary item index", e);
				return Single.error(new FormsError("Could not parse multi binary item index [%s]".formatted(index), 400));
			}
		});

		return Single.zip(formUuidSingle, formDataUuidSingle, binaryFieldNameSingle, multiBinaryIndexSingle, (formUuid, formDataUuid, binaryFieldName, index) -> formDataService.loadMultiBinaryItem(projectName, formUuid, formDataUuid, binaryFieldName, index))
			.flatMap(x -> x).doOnSuccess(meshResponse -> {
			MeshBinaryResponse downloadResponse = meshResponse.getBody();
			HttpServerResponse response = context.response();

			// Set to IDENTITY to avoid gzip compression
			// this is necessary, so that the binary data can be streamed to the client. Using gzip compression could lead to OOM when large binaries are requested
			response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);

			response.putHeader(HttpHeaders.CONTENT_LENGTH, meshResponse.getHeader("Content-Length").get());
			response.putHeader(HttpHeaders.CONTENT_TYPE, downloadResponse.getContentType());
			response.putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadResponse.getFilename() + "\"");

			downloadResponse.getFlowable().map(b -> Buffer.buffer(b)).subscribe(response::write, context::fail, response::end);

		}).ignoreElement();
	}
}
