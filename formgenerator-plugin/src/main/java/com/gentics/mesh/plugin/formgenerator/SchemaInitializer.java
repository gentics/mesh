package com.gentics.mesh.plugin.formgenerator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.dagger.BaseUrls;
import com.gentics.mesh.plugin.formgenerator.service.SchemaService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class SchemaInitializer {
	private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

	private final MeshRestClient adminClient;

	private final FormGeneratorConfig configuration;

	private final MeshPluginService meshPluginService;

	private final SchemaService schemaService;

	private final Map<String, String> baseUrls;

	private final String elementsInRootQuery;

	public static final String SCHEMA_NAME = "formgenerator";
	public static final String TEMPLATE_SCHEMA_NAME = "formgenerator_template";
	public static final String PLUGIN_SCHEMA_NAME = "formgenerator_plugin";

	@Inject
	public SchemaInitializer(@AdminClient MeshRestClient adminClient, FormGeneratorConfig configuration, MeshPluginService meshPluginService, SchemaService schemaService, @BaseUrls Map<String, String> baseUrls) {
		this.adminClient = adminClient;
		this.configuration = configuration;
		this.meshPluginService = meshPluginService;
		this.schemaService = schemaService;
		this.baseUrls = baseUrls;

		try {
			elementsInRootQuery = IOUtils.toString(getClass().getResource("elements_in_root.graphql"), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the plugin
	 * @return completable
	 */
	public Completable initialize() {
		return meshPluginService.prepareProjects(configuration.getProjects(),
			meshPluginService.ensureSchemas(SCHEMA_NAME, TEMPLATE_SCHEMA_NAME, PLUGIN_SCHEMA_NAME),
			this::initRootNode,
			this::setBaseUrl,
			this::moveFormsAndTemplates
		);
	}

	/**
	 * Ensures that the root folder exists and saves it in the rootNode map.
	 * @param projectName
	 * @return
	 */
	private Completable initRootNode(String projectName) {
		return meshPluginService.ensureFolders(projectName, PLUGIN_SCHEMA_NAME, "/FormGenerator Plugin", true)
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

	/**
	 * Move forms and templates to plugin root node for the project
	 * @param projectName project name
	 * @return completable
	 */
	public Completable moveFormsAndTemplates(String projectName) {
		String rootNodeUuid = schemaService.getRootNodeUuid(projectName);
		return adminClient.graphqlQuery(projectName, elementsInRootQuery).toSingle().flatMapCompletable(res -> {
			DocumentContext ctx = JsonPath.parse(res.toJson(), Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
			List<String> uuids = ctx.read("$.data.rootNode.children.elements[*].uuid");
			Boolean hasNextPage = ctx.read("$.data.rootNode.children.hasNextPage");

			if (uuids == null) {
				return Completable.complete();
			}
			Completable moveNodes = Observable.fromIterable(uuids).flatMapCompletable(uuid -> adminClient.moveNode(projectName, uuid, rootNodeUuid).toCompletable());
			if (hasNextPage == Boolean.TRUE) {
				moveNodes = moveNodes.andThen(moveFormsAndTemplates(projectName));
			}
			return moveNodes;
		});
	}
}
