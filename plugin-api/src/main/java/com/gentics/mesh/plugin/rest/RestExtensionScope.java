package com.gentics.mesh.plugin.rest;

/**
 * Scope of the REST extension. The extension can either be scoped globally (e.g. /api/v1/plugins/hello-world) or scoped to a project (e.g:
 * /api/v1/demo/plugins/hello-world).
 */
public enum RestExtensionScope {

	GLOBAL,

	PROJECT

}
