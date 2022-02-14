package com.gentics.mesh.plugin.forms.config;

/**
 * Different modes for marking formpages as active or inactive.
 */
public enum MultipageRenderMode {
	/** Skip to the first page that is not marked as completed. */
	SKIP_COMPLETED,
	/** Mark the page after the last submitted page as active, even if it is marked as completed. */
	FORCE_NEXT
}
