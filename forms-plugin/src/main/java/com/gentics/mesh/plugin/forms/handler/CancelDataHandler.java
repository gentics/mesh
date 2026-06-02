
package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class CancelDataHandler extends AbstractFormsHandler {
	private static final Logger log = LoggerFactory.getLogger(CancelDataHandler.class);

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
						return formDataService.load(pluginContext.projectName(), formUuid, formDataUuid, false).flatMap(
								data -> formDataService.delete(pluginContext.projectName(), formUuid, data.getUuid()));
					}
				}).doOnEvent((response, t) -> {
					if (t != null) {
						if (t instanceof FormsError formsError) {
							log.debug("Error canceling form data", t);
							pluginContext.response().setStatusCode(formsError.getStatusCode())
									.end(t.getMessage());
						} else {
							log.error("Error canceling form data", t);
							pluginContext.response().setStatusCode(500).end(t.getMessage());
						}
					} else {
						pluginContext.response().end();
					}
				}).ignoreElement();
	}
}
