package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.rest.client.MeshBinaryResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.User;

@Singleton
public class LoadFormDataBinaryHandler extends AbstractFormsHandler {

	private final FormDataService formDataService;

	@Inject
	public LoadFormDataBinaryHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		User user = context.user();

		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String formDataUuid = context.pathParam("formDataUuid");
		String binaryFieldName = context.pathParam("binaryFieldName");

		return formDataService.loadBinary(projectName, formUuid, formDataUuid, binaryFieldName).doOnSuccess(meshResponse -> {
			MeshBinaryResponse downloadResponse = meshResponse.getBody();
			HttpServerResponse response = context.response();

			// Set to IDENTITY to avoid gzip compression
			// this is necessary, so that the binary data can be streamed to the client. Using gzip compression could lead to OOM when large binaries are requested
			response.putHeader(HttpHeaders.CONTENT_ENCODING, meshResponse.getHeader("Content-Encoding").orElse(HttpHeaders.IDENTITY.toString()));
			response.putHeader(HttpHeaders.CONTENT_LENGTH, meshResponse.getHeader("Content-Length").get());
			response.putHeader(HttpHeaders.CONTENT_TYPE, downloadResponse.getContentType());
			response.putHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadResponse.getFilename() + "\"");

			downloadResponse.getFlowable().map(b -> Buffer.buffer(b)).subscribe(response::write, context::fail, response::end);

		}).ignoreElement();
	}
}
