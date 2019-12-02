package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.rest.client.MeshRequest;

public interface LocalConfigMethods {
	/**
	 * Loads the currently active local config.
	 * @return
	 */
	MeshRequest<LocalConfigModel> loadLocalConfig();

	/**
	 * Sets the local config.
	 * @param localConfigModel
	 * @return
	 */
	MeshRequest<LocalConfigModel> updateLocalConfig(LocalConfigModel localConfigModel);
}
