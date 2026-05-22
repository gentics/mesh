
package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

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
		if (StringUtils.isEmpty(formDataUuid)) {
			return Completable.error(new MeshPluginException("formDataUuid query parameter is required", 400));
		}
		return ParameterUtil.getUuidFromPath(pluginContext, "formUuid")
			// Already published data is not deletable
			.flatMap(formUuid -> formDataService.load(pluginContext.projectName(), formUuid, formDataUuid, false)
					.flatMap(data -> formDataService.delete(pluginContext.projectName(), formUuid, data.getUuid())))
			.doOnEvent((response, t) -> {
				if (t != null) {
					if (t instanceof FormsError formsError) {
						pluginContext.response().setStatusCode(formsError.getStatusCode()).end(t.getLocalizedMessage());
					} else {
						pluginContext.response().setStatusCode(500).end(t.getLocalizedMessage());
					}
				} else {
					pluginContext.response().end();
				}
			}).ignoreElement();
	}
}
