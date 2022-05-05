package com.gentics.mesh.monitor.liveness;

/**
 * Liveness manager for the vert.x eventBus.
 * If enabled, this will do regular checks for the eventBus by publishing events, which are handled locally (and cluster-wide if clustering is enabled).
 * The regular check will log warn or error messages, if the last ping was received longer than warnThreshold or errorThreshold ago.
 * If the last ping was received longer than errorThreshold ago, the liveness will also be set to "false".
 */
public interface EventBusLivenessManager {
	/**
	 * Start the regular checks, if checkInterval is greater than 0
	 */
	void startRegularChecks();
}
