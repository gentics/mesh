package com.gentics.mesh.core.verticle.admin.consistency;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;

/**
 * A consistency check must identify and log database inconsistencies.
 */
public interface ConsistencyCheck {

	/**
	 * Invoke the consistency check and update the given response with found inconsistencies.
	 * 
	 * @param boot
	 * @param response
	 */
	void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response);

}
