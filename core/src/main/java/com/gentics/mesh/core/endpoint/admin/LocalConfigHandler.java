package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.util.VertxUtil.restModelSender;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.admin.runtimeconfig.LocalConfigModel;
import com.gentics.mesh.json.JsonUtil;

@Singleton
public class LocalConfigHandler {

	private final LocalConfigApi localConfigApi;

	@Inject
	public LocalConfigHandler(LocalConfigApi localConfigApi) {
		this.localConfigApi = localConfigApi;
	}

	public void handleGetActiveConfig(InternalActionContext rc) {
		localConfigApi.getActiveConfig().subscribe(restModelSender(rc));
	}

	public void handleSetActiveConfig(InternalActionContext rc) {
		LocalConfigModel model = JsonUtil.readValue(rc.getBodyAsString(), LocalConfigModel.class);
		localConfigApi.setActiveConfig(model).subscribe(restModelSender(rc));
	}
}
