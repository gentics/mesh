package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.rest.client.MeshBinaryResponse;

import io.reactivex.Completable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class LoadFormDataBinaryHandler extends AbstractFormsHandler {

	private static final Logger log = LoggerFactory.getLogger(LoadFormDataBinaryHandler.class);

	private final FormDataService formDataService;

	@Inject
	public LoadFormDataBinaryHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String formDataUuid = context.pathParam("formDataUuid");
		String binaryFieldName = context.pathParam("binaryFieldName");

		return formDataService.loadBinary(projectName, formUuid, formDataUuid, binaryFieldName).doOnSuccess(meshResponse -> {
			MeshBinaryResponse downloadResponse = meshResponse.getBody();
			HttpServerResponse response = context.response();
			response.headers().add("Content-Type", downloadResponse.getContentType());
			response.headers().add("Content-Length", meshResponse.getHeader("Content-Length").get());
			response.headers().add("Content-Disposition", "attachment; filename=\"" + downloadResponse.getFilename() + "\"");
			downloadResponse.getFlowable().subscribe(b -> {
				response.write(Buffer.buffer(b));
			}, err -> {
				log.error("Error while reading download for {" + formUuid + "} {" + formDataUuid + "} {" + binaryFieldName + "}", err);
			}, () -> {
				response.end();
			});
		}).ignoreElement();
	}
}
