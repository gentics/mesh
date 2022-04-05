package com.gentics.mesh.plugin.forms;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Schema Initializer for creating the forms schemas and preparing the root nodes in projects, where the plugin is activated
 */
@Singleton
public class SchemaInitializer {
	private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

	private final MeshRestClient adminClient;

	private final MeshPluginService meshPluginService;

	public static final String SCHEMA_NAME = "form";
	public static final String PLUGIN_SCHEMA_NAME = "forms_plugin";

	@Inject
	public SchemaInitializer(@AdminClient MeshRestClient adminClient, MeshPluginService meshPluginService) {
		this.adminClient = adminClient;
		this.meshPluginService = meshPluginService;
	}

	/**
	 * Initialize the plugin for a project
	 * @param projectName project
	 * @return completable
	 */
	public Completable initialize(String projectName) {
		return meshPluginService.prepareProject(projectName,
			meshPluginService.ensureSchemas(SCHEMA_NAME, PLUGIN_SCHEMA_NAME),
			this::initRootNode
		);
	}

	/**
	 * Ensures that the root folder exists and saves it in the rootNode map.
	 * @param projectName
	 * @return
	 */
	private Completable initRootNode(String projectName) {
		return meshPluginService.ensureFolders(projectName, PLUGIN_SCHEMA_NAME, "/Forms Plugin", true)
			.ignoreElement();
	}
}
