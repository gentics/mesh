package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;

@Singleton
public class DeleteFormHandler extends AbstractFormsHandler {

	private final FormService formService;

	@Inject
	public DeleteFormHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		Single<String> singleUuid = ParameterUtil.getUuidFromPath(context, "formUuid");

		return singleUuid.flatMap(uuid -> {
			return formService.delete(context.project().getString("name"), uuid);
		}).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
						.setStatusCode(204).end();
			} else if (t instanceof MeshRestClientMessageException restClientMessageException) {
				if (restClientMessageException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
					context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
							.setStatusCode(204).end();
				}
			}
		}).ignoreElement();
	}
}
