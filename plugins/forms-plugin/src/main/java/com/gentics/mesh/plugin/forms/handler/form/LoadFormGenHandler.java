package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.AuthUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

/**
 * Handles {@code POST /forms/:formUuid/load} for the Form Generator.
 *
 * <p>Builds a {@link com.gentics.mesh.plugin.forms.model.Form} response whose {@code data} section
 * is populated either from schema default values (no {@code formDataUuid}) or from a previously
 * stored form-data submission node ({@code formDataUuid} passed as query parameter).</p>
 */
@Singleton
public class LoadFormGenHandler extends AbstractFormsHandler {

	private final FormService formService;
	private final FormDataService formDataService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;
	private final ObjectMapper objectMapper;

	@Inject
	public LoadFormGenHandler(FormService formService, FormDataService formDataService, StatusService statusService,
			@AdminClient MeshRestClient adminClient, ObjectMapper objectMapper) {
		this.formService = formService;
		this.formDataService = formDataService;
		this.statusService = statusService;
		this.adminClient = adminClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<FormtypeConfigsModel> formtypeConfigsSingle = statusService.getFormtypeConfigs(projectName, adminClient);

		JsonObject body = context.body().asJsonObject();
		String formID = body != null ? body.getString("formID") : null;
		String formDataUuid = ParameterUtil.getQueryParam(context, "formDataUuid");
		Map<String, Object> jwtClaims = AuthUtil.decodeToken(context).getMap();

		return Single.zip(uuidSingle, formtypeConfigsSingle, (uuid, formtypeConfigs) -> ParameterUtil.getFirstLanguage(context, "dump")
						.flatMap(lang -> {
							if ("dump".equals(lang)) {
								return Single.error(new MeshPluginException("lang query parameter is required", 400));
							}
							// try to load the form data node, but ignore if that fails (e.g. when the form data node does not exist)
							Single<Optional<NodeResponse>> formDataNodeSingle = formDataUuid != null
									? formDataService.load(projectName, uuid, formDataUuid, true).map(Optional::of).onErrorResumeNext(e -> Single.just(Optional.empty()))
									: Single.just(Optional.empty());
					return formDataNodeSingle.flatMap(opt -> {
						// when the form data node does not exist (or none was requested), clear the data cookie
						if (opt.isEmpty()) {
							formDataService.removeDataCookie(context);
						}
						return formService.loadFormForFormGen(formtypeConfigs, projectName, uuid, formID, lang,
								opt.orElse(null), jwtClaims);
					});
						}))
				.flatMap(x -> x)
				.map(form -> {
					try {
						return objectMapper.writeValueAsString(form);
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
				})
				.doOnEvent((json, t) -> {
					if (json != null) {
						context.response()
								.putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
								.end(json);
					}
				})
				.ignoreElement();
	}
}
