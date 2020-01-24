package com.gentics.mesh.plugin.formgenerator.service;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.formgenerator.FormGeneratorPlugin;
import com.gentics.mesh.plugin.formgenerator.SchemaInitializer;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Singleton
public class TemplateService {
	private final MeshRestClient adminClient;

	private final SchemaService schemaService;

	@Inject
	public TemplateService(@AdminClient MeshRestClient adminClient, SchemaService schemaService) {
		this.adminClient = adminClient;
		this.schemaService = schemaService;
	}

	/**
	 * List all templates
	 * 
	 * @param projectName project name
	 * @return single response
	 */
	public Single<JsonObject> list(String projectName) {
		try {
			String graphQlQuery = IOUtils.toString(FormGeneratorPlugin.class.getResource("templates.graphql"), "UTF-8");
			return adminClient.graphql(projectName, new GraphQLRequest().setQuery(graphQlQuery)).toSingle().flatMap(graphQlResponse -> {
				// make response flat
				JsonArray globalTemplates = graphQlResponse.getData().getJsonObject("schema").getJsonObject("nodes").getJsonArray("elements");
				for (int i = 0; i < globalTemplates.size(); i++) {
					JsonObject element = globalTemplates.getJsonObject(i);
					element.put("id", element.getString("uuid"));
					element.put("name", element.getJsonObject("fields").getString("name"));
					element.put("json", element.getJsonObject("fields").getString("json"));
				}
				JsonArray userTemplates = new JsonArray();
				return Single.just(new JsonObject().put("templates", new JsonObject().put("global", globalTemplates).put("user", userTemplates)));
			});
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Create a new template
	 *
	 * @param projectName project name
	 * @param templateData json template data
	 * @return single node response
	 */
	public Single<JsonObject> create(String projectName, JsonObject templateData) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setSchemaName(SchemaInitializer.TEMPLATE_SCHEMA_NAME)
			.setParentNodeUuid(schemaService.getRootNodeUuid(projectName))
			.setLanguage("en");

		request.getFields().put("name", new StringFieldImpl().setString(templateData.getString("name")));
		request.getFields().put("json", new StringFieldImpl().setString(templateData.getString("json")));
		// create the form_template node and publish it
		return adminClient.createNode(projectName, request).toSingle().flatMap(createdFormTemplateNode -> {
			return adminClient.publishNode(projectName, createdFormTemplateNode.getUuid()).toSingle().map(r -> createdFormTemplateNode);
		}).flatMap(this::transform);
	}

	/**
	 * Load a template
	 *
	 * @param projectName project name
	 * @param uuid
	 *            template uuid
	 */
	public Single<JsonObject> load(String projectName, String uuid) {
		return adminClient.findNodeByUuid(projectName, uuid).toSingle().flatMap(this::transform);
	}

	/**
	 * Delete the template with given uuid
	 * 
	 * @param projectName
	 *            project name
	 * @param uuid
	 *            uuid
	 * @return single void
	 */
	public Completable delete(String projectName, String uuid) {
		return adminClient.findNodeByUuid(projectName, uuid).toSingle().flatMapCompletable(foundNode -> {
			if (SchemaInitializer.TEMPLATE_SCHEMA_NAME.equals(foundNode.getSchema().getName())) {
				return adminClient.deleteNode(projectName, uuid).toCompletable();
			} else {
				return Completable.complete();
			}
		});
	}

	/**
	 * Transform a Mesh node to a template object
	 * @param templateNode mesh node
	 * @return single template object
	 */
	protected Single<JsonObject> transform(NodeResponse templateNode) {
		JsonObject templateJsonObject = new JsonObject().put("id", templateNode.getUuid())
				.put("name", templateNode.getFields().getStringField("name").getString())
				.put("json", templateNode.getFields().getStringField("json").getString());
		return Single.just(templateJsonObject);
	}
}
