package com.gentics.mesh.plugin.like;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.like.config.LikePluginConfig;
import com.gentics.mesh.plugin.like.dagger.AdminClient;
import com.gentics.mesh.plugin.like.service.SchemaService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SchemaInitializer {
	private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

	private final MeshRestClient adminClient;

	private final LikePluginConfig configuration;

	private final SchemaService schemaService;

	@Inject
	public SchemaInitializer(@AdminClient MeshRestClient adminClient, LikePluginConfig configuration, SchemaService schemaService) {
		this.adminClient = adminClient;
		this.configuration = configuration;
		this.schemaService = schemaService;
	}

	/**
	 * Initialize the plugin
	 * @return completable
	 */
	public Completable initialize() {
		return initSchemas().andThen(initProjects());
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
		return Observable.fromIterable(schemaService.getSchemaNames())
				.flatMapCompletable(name -> checkSchema(name).flatMap(schema -> assignToProjects(schema)).ignoreElement());
	}

	/**
	 * Check existence and validity of schema
	 * @param name schema name
	 * @return single schema response
	 */
	private Single<SchemaResponse> checkSchema(String name) {
		try {
			String filename = String.format("/schemas/%s.json", name);
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
					return initPluginRootNode(projectResponse).flatMapCompletable(rootNodeUuid -> {
						schemaService.setRootNodeUuid(projectResponse.getName(), rootNodeUuid);
						return Completable.complete();
					});
				});
	}

	/**
	 * Initialize plugin root node for project
	 * @param project project
	 * @return Single UUID of plugin root node
	 */
	private Single<String> initPluginRootNode(ProjectResponse project) {
		try {
			String graphQl = IOUtils.toString(getClass().getResource("/queries/like_plugin_node.graphql"), "UTF-8");
			return adminClient.graphqlQuery(project.getName(), graphQl).toSingle().flatMap(res -> {
				// TODO check for errors
				DocumentContext ctx = JsonPath.parse(res.toJson(), Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
				String uuid = ctx.read("$.data.schema.nodes.elements[0].uuid");

				if (uuid == null) {
					NodeCreateRequest create = new NodeCreateRequest()
							.setSchemaName(SchemaService.PLUGIN_SCHEMA_NAME)
							.setParentNode(project.getRootNode())
							.setLanguage("en");
					create.getFields().put("name", new StringFieldImpl().setString(LikePlugin.NAME));
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
}
