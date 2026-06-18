package com.gentics.mesh.plugin.forms.handler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.model.schema.BinaryData;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.rest.client.MeshBinaryResponse;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;

/**
 * Handler for loading an uploaded binary data item
 */
@Singleton
public class LoadBinaryHandler extends AbstractFormsHandler {

	private static final Logger log = LoggerFactory.getLogger(LoadBinaryHandler.class);

	private final FormDataService formDataService;

	@Inject
	public LoadBinaryHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	public static Single<String> getMeshBinaryUuid(PluginContext context) {
		String value = URLDecoder.decode(StringUtils.trimToEmpty(context.pathParam("meshBinaryUuid")), StandardCharsets.UTF_8);

		Matcher matcher = BinaryData.MESH_ID_REGEX.matcher(value);
		if (matcher.matches()) {
			return Single.just(matcher.group(1));
		} else {
			return Single.error(ParameterUtil.illegalPathParameter("meshBinaryUuid"));
		}
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> binaryUuidSingle = getMeshBinaryUuid(context);

		String width = StringUtils.trimToEmpty(ParameterUtil.getQueryParam(context, "w"));
		String height = StringUtils.trimToEmpty(ParameterUtil.getQueryParam(context, "h"));

		return Single.zip(formUuidSingle, binaryUuidSingle, (formUuid, binaryUuid) -> {
			return formDataService.loadBinaryForFormGen(projectName, formUuid, binaryUuid, width, height);
		}).flatMap(x -> x).doOnSuccess(meshResponse -> {
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
