package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.util.VertxUtil.restModelSender;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.json.JsonUtil;

/**
 * Handler allows setting and getting the local configuration.
 */
@Singleton
public class LocalConfigHandler {

	private final LocalConfigApiImpl localConfigApi;

	@Inject
	public LocalConfigHandler(LocalConfigApiImpl localConfigApi) {
		this.localConfigApi = localConfigApi;
	}

	/**
	 * Return the local active config.
	 * 
	 * @param rc
	 */
	public void handleGetActiveConfig(InternalActionContext rc) {
		localConfigApi.getActiveConfig().subscribe(restModelSender(rc));
	}

	/**
	 * Update the local active config.
	 * 
	 * @param rc
	 */
	public void handleSetActiveConfig(InternalActionContext rc) {
		LocalConfigModel model = JsonUtil.readValue(rc.getBodyAsString(), LocalConfigModel.class);
		localConfigApi.setActiveConfig(model).subscribe(restModelSender(rc));
	}
}
