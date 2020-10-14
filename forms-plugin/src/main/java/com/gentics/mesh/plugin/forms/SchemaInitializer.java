package com.gentics.mesh.plugin.forms;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.forms.dagger.BaseUrls;
import com.gentics.mesh.plugin.forms.service.SchemaService;
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

	private final SchemaService schemaService;

	private final Map<String, String> baseUrls;

	public static final String SCHEMA_NAME = "form";
	public static final String PLUGIN_SCHEMA_NAME = "forms_plugin";

	@Inject
	public SchemaInitializer(@AdminClient MeshRestClient adminClient, MeshPluginService meshPluginService, SchemaService schemaService, @BaseUrls Map<String, String> baseUrls) {
		this.adminClient = adminClient;
		this.meshPluginService = meshPluginService;
		this.schemaService = schemaService;
		this.baseUrls = baseUrls;
	}

	/**
	 * Initialize the plugin for a project
	 * @param projectName project
	 * @return completable
	 */
	public Completable initialize(String projectName) {
		return meshPluginService.prepareProject(projectName,
			meshPluginService.ensureSchemas(SCHEMA_NAME, PLUGIN_SCHEMA_NAME),
			this::initRootNode,
			this::setBaseUrl
		);
	}

	/**
	 * Ensures that the root folder exists and saves it in the rootNode map.
	 * @param projectName
	 * @return
	 */
	private Completable initRootNode(String projectName) {
		return meshPluginService.ensureFolders(projectName, PLUGIN_SCHEMA_NAME, "/Forms Plugin", true)
			.doOnSuccess(node -> schemaService.setRootNodeUuid(projectName, node.getUuid()))
			.ignoreElement();
	}

	/**
	 * Gets the base url of the projects and saves it in {@link #baseUrls}.
	 *
	 * @param projectName
	 * @return
	 */
	private Completable setBaseUrl(String projectName) {
		return adminClient.findProjectByName(projectName).toSingle()
			.flatMap(projectResponse -> {
				String link = String.format("{{mesh.link(\"%s\", \"en\")}}", projectResponse.getRootNode().getUuid());
				ParameterProvider params = new NodeParametersImpl().setResolveLinks(LinkType.SHORT);

				return adminClient.resolveLinks(link, params).toSingle();
			}).doOnSuccess(baseUrl -> baseUrls.put(projectName, StringUtils.removeEnd(baseUrl, "/")))
			.ignoreElement();
	}
}
