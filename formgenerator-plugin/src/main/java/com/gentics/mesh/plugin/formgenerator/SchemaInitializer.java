package com.gentics.mesh.plugin.formgenerator;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.dagger.AdminClient;
import com.gentics.mesh.plugin.formgenerator.dagger.BaseUrls;
import com.gentics.mesh.plugin.formgenerator.service.SchemaService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Singleton
public class SchemaInitializer {
	private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

	private final MeshRestClient adminClient;

	private final FormGeneratorConfig configuration;

	private final SchemaService schemaService;

	private final Map<String, String> baseUrls;
	@Inject
	public SchemaInitializer(@AdminClient MeshRestClient adminClient, FormGeneratorConfig configuration, SchemaService schemaService, @BaseUrls Map<String, String> baseUrls) {
		this.adminClient = adminClient;
		this.configuration = configuration;
		this.schemaService = schemaService;
		this.baseUrls = baseUrls;
	}

	/**
	 * Initialize the plugin
	 * @return completable
	 */
	public Completable initialize() {
		return initSchemas().andThen(initProjects()).andThen(moveFormsAndTemplates());
	}

	/**
	 * Initialize schemas.<br>
	 * For each schema:
	 * <ol>
	 * <li>Check that schema exists and is correct (if not, create or update)</li>
	 * <li>Assign schema to projects</li>
	 * </ol>
	 * @return completable
	 */
	private Completable initSchemas() {
		return Observable.fromArray(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME, SchemaService.TEMPLATE_SCHEMA_NAME)
				.flatMapCompletable(name -> checkSchema(name).flatMap(schema -> assignToProjects(schema)).ignoreElement());
	}

	/**
	 * Check existence and validity of schema
	 * @param name schema name
	 * @return single schema response
	 */
	private Single<SchemaResponse> checkSchema(String name) {
		try {
			String filename = String.format("%s_schema.json", name);
			log.info("Loading schema {" + filename + "}");
			String schemaJson = IOUtils.toString(getClass().getResource(filename), "UTF-8");

			return schemaService.loadSchemaByName(name, false).flatMap(schema -> {
				if (!schema.isPresent()) {
					log.info(String.format("Schema %s not found, creating", name));
					// create and assign to project
					SchemaCreateRequest request = JsonUtil.readValue(schemaJson, SchemaCreateRequest.class);
					return adminClient.createSchema(request).toSingle();
				} else {
					log.info(String.format("Schema %s found, updating", name));
					// update
					SchemaUpdateRequest request = JsonUtil.readValue(schemaJson, SchemaUpdateRequest.class);
					return adminClient.updateSchema(schema.get().getUuid(), request).toSingle().flatMap(r -> Single.just(schema.get()));
				}
			});
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Assign schema to projects
	 * @param schema schema response
	 * @return single schema response
	 */
	private Single<SchemaResponse> assignToProjects(SchemaResponse schema) {
		return Observable.fromIterable(configuration.getProjects()).flatMapSingle(name -> adminClient.assignSchemaToProject(name, schema.getUuid()).toSingle())
				.lastOrError();
	}

	/**
	 * Initialize projects.<br>
	 * For each configured project:
	 * <ol>
	 * <li>Check whether plugin root node exists (if not, create)</li>
	 * <li>Store UUID of plugin root node</li>
	 * </ol>
	 * @return completable
	 */
	private Completable initProjects() {
		return Observable.fromIterable(configuration.getProjects()).flatMapSingle(name -> adminClient.findProjectByName(name).toSingle())
				.flatMapCompletable(projectResponse -> {
					String link = String.format("{{mesh.link(\"%s\", \"en\")}}", projectResponse.getRootNode().getUuid());
					ParameterProvider params = new NodeParametersImpl().setResolveLinks(LinkType.SHORT);

					return adminClient.resolveLinks(link, params).toSingle()
						.doOnSuccess(baseUrl -> baseUrls.put(projectResponse.getName(), StringUtils.removeEnd(baseUrl, "/"))).ignoreElement()
						.andThen(initPluginRootNode(projectResponse)
							.flatMapCompletable(rootNodeUuid -> {
								schemaService.setRootNodeUuid(projectResponse.getName(), rootNodeUuid);
								return Completable.complete();
							}));
				});
	}

	/**
	 * Initialize plugin root node for project
	 * @param project project
	 * @return Single UUID of plugin root node
	 */
	private Single<String> initPluginRootNode(ProjectResponse project) {
		try {
			String graphQl = IOUtils.toString(FormGeneratorPlugin.class.getResource("formgenerator_plugin_node.graphql"), "UTF-8");
			return adminClient.graphqlQuery(project.getName(), graphQl).toSingle().flatMap(res -> {
				// TODO check for errors
				DocumentContext ctx = JsonPath.parse(res.toJson(), Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
				String uuid = ctx.read("$.data.schema.nodes.elements[0].uuid");

				if (uuid == null) {
					NodeCreateRequest create = new NodeCreateRequest()
							.setSchemaName(SchemaService.PLUGIN_SCHEMA_NAME)
							.setParentNode(project.getRootNode())
							.setLanguage("en");
					create.getFields().put("name", new StringFieldImpl().setString("FormGenerator Plugin"));
					return adminClient.createNode(project.getName(), create).toSingle()
							.flatMap(created -> adminClient.publishNode(project.getName(), created.getUuid()).toSingle().map(status -> created))
							.map(response -> response.getUuid());
				} else {
					return Single.just(uuid);
				}
			});
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Move forms and templates to plugin root node for all projects
	 * @return completable
	 */
	private Completable moveFormsAndTemplates() {
		try {
			String graphQl = IOUtils.toString(FormGeneratorPlugin.class.getResource("elements_in_root.graphql"), "UTF-8");
			return Observable.fromIterable(configuration.getProjects()).flatMapCompletable(name -> moveFormsAndTemplates(name, graphQl));
		} catch (IOException e) {
			return Completable.error(e);
		}
	}

	/**
	 * Move forms and templates to plugin root node for the project
	 * @param projectName project name
	 * @param graphQl graph ql to load all forms and templates that need to be moved
	 * @return completable
	 */
	private Completable moveFormsAndTemplates(String projectName, String graphQl) {
		String rootNodeUuid = schemaService.getRootNodeUuid(projectName);
		return adminClient.graphqlQuery(projectName, graphQl).toSingle().flatMapCompletable(res -> {
			DocumentContext ctx = JsonPath.parse(res.toJson(), Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
			List<String> uuids = ctx.read("$.data.rootNode.children.elements[*].uuid");
			Boolean hasNextPage = ctx.read("$.data.rootNode.children.hasNextPage");

			if (uuids == null) {
				return Completable.complete();
			}
			Completable moveNodes = Observable.fromIterable(uuids).flatMapCompletable(uuid -> adminClient.moveNode(projectName, uuid, rootNodeUuid).toCompletable());
			if (hasNextPage == Boolean.TRUE) {
				moveNodes = moveNodes.andThen(moveFormsAndTemplates(projectName, graphQl));
			}
			return moveNodes;
		});
	}
}
