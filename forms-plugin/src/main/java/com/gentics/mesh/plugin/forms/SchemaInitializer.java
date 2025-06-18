package com.gentics.mesh.plugin.forms;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.github.jknack.handlebars.internal.lang3.StringUtils;

import io.reactivex.Completable;

/**
 * Schema Initializer for creating the forms schemas and preparing the root nodes in projects, where the plugin is activated
 */
@Singleton
public class SchemaInitializer {

	public static final String PATH_FORMS_PLUGIN_ROOT = "/Forms Plugin";
	public static final String PATH_DOWNLOADS_SEGMENT = "/Downloads";

	private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

	private final MeshRestClient adminClient;

	private final MeshPluginService meshPluginService;

	public static final String SCHEMA_NAME = "form";
	public static final String DOWNLOAD_SCHEMA_NAME = "form_download";
	public static final String FOLDER_SCHEMA_NAME = "form_folder";
	public static final String PLUGIN_SCHEMA_NAME = "forms_plugin";

	public static final List<String> PLUGIN_SCHEMAS = List.of(SCHEMA_NAME, DOWNLOAD_SCHEMA_NAME, PLUGIN_SCHEMA_NAME, FOLDER_SCHEMA_NAME);

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
	@Deprecated
	public Completable initialize(String projectName) {
		return initialize(projectName, Optional.empty());
	}

	/**
	 * Initialize the plugin for a project, after the global schema initialization, if available
	 * @param projectName project
	 * @param maybeExistingSchemas the already checked schemas, if available
	 * @return completable
	 */
	public Completable initialize(String projectName, Optional<SchemaListResponse> maybeExistingSchemas) {
		return meshPluginService.prepareProject(projectName,
			meshPluginService.ensureSchemas(StringUtils.EMPTY, PLUGIN_SCHEMAS, maybeExistingSchemas),
			this::initRootNode
		);
	}

	/**
	 * Ensures that the root folder exists and saves it in the rootNode map.
	 * @param projectName
	 * @return
	 */
	private Completable initRootNode(String projectName) {
		return meshPluginService.ensureFolders(projectName, PLUGIN_SCHEMA_NAME, PATH_FORMS_PLUGIN_ROOT, true).ignoreElement();
	}
}
