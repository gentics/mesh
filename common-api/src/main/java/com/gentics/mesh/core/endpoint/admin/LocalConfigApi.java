package com.gentics.mesh.core.endpoint.admin;

import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;

import io.reactivex.Single;

/**
 * Contract to process the local configuration.
 */
public interface LocalConfigApi {

	/**
	 * Loads the local config currently active in this instance.
	 * @return
	 */
	Single<LocalConfigModel> getActiveConfig();

}