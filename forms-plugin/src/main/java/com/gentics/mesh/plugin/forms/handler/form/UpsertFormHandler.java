package com.gentics.mesh.plugin.forms.handler.form;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

@Singleton
public class UpsertFormHandler extends AbstractFormsHandler {
	private final FormService formService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;

	@Inject
	public UpsertFormHandler(FormService formService, StatusService statusService,
			@AdminClient MeshRestClient adminClient) {
		this.formService = formService;
		this.statusService = statusService;
		this.adminClient = adminClient;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<FormtypeConfigsModel> formtypeConfigsSingle = statusService.getFormtypeConfigs(projectName, adminClient);
		JsonObject formData = context.body().asJsonObject();

		if (formData != null && formData.containsKey("ui-schema")) {
			formData.put("uiSchema", formData.remove("ui-schema"));
		}

		return Single
				.zip(uuidSingle, formtypeConfigsSingle,
						(uuid, formtypeConfigs) -> formService.upsert(formtypeConfigs, projectName, uuid, formData, "en"))
				.flatMap(x -> x).doOnEvent((response, t) -> {
					if (response != null) {
						context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
								.end(response.toJson());
					}
				}).ignoreElement();
	}
}
