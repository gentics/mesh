
package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class CancelDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public CancelDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext pluginContext) {
		String formDataUuid = ParameterUtil.getQueryParam(pluginContext, "formDataUuid");
		return ParameterUtil.getUuidFromPath(pluginContext, "formUuid")
			// Already published data is not deletable
				.flatMap(formUuid -> {
					if (StringUtils.isEmpty(formDataUuid)) {
						return Single.just(EmptyResponse.getInstance());
					} else {
						return formDataService.cancel(pluginContext.projectName(), formUuid, formDataUuid);
					}
				}).doOnEvent((response, t) -> {
					if (t != null) {
						if (t instanceof FormsError formsError) {
							pluginContext.response().setStatusCode(formsError.getStatusCode())
									.end(t.getLocalizedMessage());
						} else {
							pluginContext.response().setStatusCode(500).end(t.getLocalizedMessage());
						}
					} else {
						pluginContext.response().end();
					}
				}).ignoreElement();
	}
}
