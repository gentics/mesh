package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.model.response.RenderResponse;
import com.gentics.mesh.portal.i18n.I18nHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

@Singleton
public class FormService {
	private static final Logger log = LoggerFactory.getLogger(FormService.class);

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final ObjectMapper objectMapper;

	private final SchemaService schemaService;

	private final PagingService pagingService;

	private final HandlebarsTemplateEngine templateEngine;

	private final FormsConfig configuration;

	private final StatusService statusService;

	@Inject
	public FormService(@AdminClient MeshRestClient adminClient, @UserClient @Nullable MeshRestClient userClient, ObjectMapper objectMapper, SchemaService schemaService, PagingService pagingService,
					   HandlebarsTemplateEngine templateEngine, FormsConfig configuration, StatusService statusService) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.objectMapper = objectMapper;
		this.schemaService = schemaService;
		this.pagingService = pagingService;
		this.templateEngine = templateEngine;
		this.configuration = configuration;
		this.statusService = statusService;
	}

	/**
	 * Create a new form
	 * @param projectName project name
	 * @param formData posted form data
	 * @return single of the node response
	 */
	public Single<NodeResponse> create(String projectName, JsonObject formData) {
		Optional<ValidationError> error = checkForm(formData);

		if (error.isPresent()) {
			return Single.error(
				new FormsError(HttpResponseStatus.BAD_REQUEST.code(), error.get().getMessage()));
		}

		NodeCreateRequest request = new NodeCreateRequest()
				.setSchemaName(SchemaInitializer.SCHEMA_NAME)
				.setParentNodeUuid(schemaService.getRootNodeUuid(projectName))
				.setLanguage("en");

		request.getFields().put("name", new StringFieldImpl().setString(formData.getString("name")));
		request.getFields().put("json", new StringFieldImpl().setString(formData.encodePrettily()));
		String pageIdString = formData.getString("pageId", null);
		if (pageIdString != null) {
			try {
				int pageId = Integer.parseInt(pageIdString);
				request.getFields().put("pageId", new NumberFieldImpl().setNumber(pageId));
			} catch (NumberFormatException e) {
				log.error(String.format("Error while saving form: %s is not a valid pageId", pageIdString));
			}
		}
		// create the form node, then create the schema and assign to the project
		return userClient.createNode(projectName, request).toSingle().flatMap(createdFormNode -> {
			SchemaCreateRequest schema = getSchema(createdFormNode, new SchemaCreateRequest());
			return userClient.createSchema(schema).toSingle().flatMap(createdSchema -> {
				return userClient.assignSchemaToProject(projectName, createdSchema.getUuid()).toSingle().map(r -> createdFormNode);
			});
		}).flatMap(createdFormNode -> {
			return userClient.publishNode(projectName, createdFormNode.getUuid()).toSingle().map(r -> createdFormNode);
		});
	}

	/**
	 * Validate the form itself before sending a create or update request.
	 *
	 * <p>
	 *     Only elements with types configured in `validatedElements` will be checked, which is empty in the default
	 *     configuration.
	 * </p>
	 *
	 * @param formData The received form data
	 * @return The respective error, when the form validation failed.
	 */
	private Optional<ValidationError> checkForm(JsonObject formData) {
		if (!configuration.isValidateElements()) {
			return Optional.empty();
		}

		try {
			return toForm(formData.encode(), formData.getString("uuid")).validateForm();
		} catch (IOException e) {
			log.error("Could not deserialize form: {}", e, e.getMessage());

			if (log.isTraceEnabled()) {
				log.trace("Received formData:\n{}", formData.encodePrettily());

			}
		}

		ValidationError error = new ValidationError()
			.setName("form")
			.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.form.title"))
			.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.form.bad_request"));

		return Optional.of(error);
	}

	/**
	 * Load the form node by uuid
	 *
	 * @param projectName project name
	 * @param uuid
	 *            uuid
	 * @return single noderesponse
	 */
	public Single<NodeResponse> load(String projectName, String uuid) {
		return userClient.findNodeByUuid(projectName, uuid).toSingle().flatMap(nodeResponse -> {
			if (isFormNode(nodeResponse)) {
				return Single.just(nodeResponse);
			} else {
				String message = String.format("Object with uuid \"%s\" could not be found.", uuid);
				return Single.error(new FormsError(HttpResponseStatus.NOT_FOUND.code(), message));
			}
		});
	}

	/**
	 * Load the form node by uuid as admin user
	 *
	 * @param projectName project name
	 * @param uuid
	 *            uuid
	 * @param lang optional languages
	 * @return single noderesponse
	 */
	public Single<NodeResponse> loadAsAdmin(String projectName, String uuid, String...lang) {
		NodeParameters nParams = new NodeParametersImpl();
		if (lang.length > 0) {
			nParams.setLanguages(lang);
		}
		VersioningParameters vParams = new VersioningParametersImpl().setVersion("published");
		return adminClient.findNodeByUuid(projectName, uuid, nParams, vParams).toSingle().flatMap(nodeResponse -> {
			if (isFormNode(nodeResponse)) {
				return Single.just(nodeResponse);
			} else {
				String message = String.format("Object with uuid \"%s\" could not be found.", uuid);
				return Single.error(new FormsError(HttpResponseStatus.NOT_FOUND.code(), message));
			}
		});
	}

	/**
	 * Get the elements JSON for the form
	 * @param projectName project name
	 * @param uuid uuid
	 * @return single JsonArray containing the elements
	 */
	public Single<JsonObject> getElements(String projectName, String uuid) {
		return load(projectName, uuid).flatMap(formNode -> {
			JsonObject formJson = new JsonObject(formNode.getFields().getStringField("json").getString());
			return Single.just(new JsonObject().put("elements", formJson.getJsonArray("elements")));
		});
	}

	/**
	 * Retrieves all forms (optionally paged) and sends them to the client in the format:
	 *
	 * <pre>
	   {
			"totalCount" : 4,
			"currentPage" : 1,
			"pageCount" : 1,
			"perPage" : 1000000,
			"forms" : [ {
				 "uuid" : "29a53fee7af6468da53fee7af6568d75",
				 "name" : "Formular Test 1",
				 "pageId" : 1234
			 }, {
				 "uuid" : "05a0c76f7d5940dfa0c76f7d5960df23",
				 "name" : "Formular Nr. 2",
				 "pageId" : 4711
			 }, {
				 "uuid" : "5a1e034b14ef480f9e034b14efa80fe3",
				 "name" : "Formular Nr. 3",
				 "pageId" : 1289
			 }, {
				 "uuid" : "33b96c62cb754b74b96c62cb75db74bb",
				 "name" : "Formular Nr. 4",
				 "pageId" : 4321
			 } ]
	   }
	 * </pre>
	 * @param projectName project name
	 * @param pagingParams paging parameters
	 * @return single json object
	 */
	public Single<JsonObject> list(String projectName, JsonObject pagingParams) {
		try {
			String graphQlQuery = IOUtils.toString(FormsPlugin.class.getResource("forms.graphql"), StandardCharsets.UTF_8);

			return userClient.findNodeByUuid(projectName, schemaService.getRootNodeUuid(projectName)).toSingle()
				.flatMap(rootNode -> userClient.graphql(projectName, new GraphQLRequest().setQuery(graphQlQuery).setVariables(pagingParams)).toSingle())
				.flatMap(graphQlResponse -> {
					JsonObject clientResponse = new JsonObject();
					if (graphQlResponse.getData() == null) {
						// TODO
						return Single.error(new Exception());
					}
					if (!graphQlResponse.getData().containsKey("schema")) {
						// TODO
						return Single.error(new Exception());
					}
					JsonObject schema = graphQlResponse.getData().getJsonObject("schema");
					if (!schema.containsKey("nodes")) {
						// TODO
						return Single.error(new Exception());
					}
					JsonObject nodes = schema.getJsonObject("nodes");
					if (nodes == null) {
						// TODO
						return Single.error(new Exception());
					}
					pagingService.addPagingInfoToResponse(clientResponse, nodes);

					// make response flat
					JsonArray forms = nodes.getJsonArray("elements");
					List<JsonObject> clientForms = new ArrayList<>(forms.size());
					for (int i = 0; i < forms.size(); i++) {
						JsonObject form = forms.getJsonObject(i);
						JsonObject clientForm = new JsonObject();
						clientForm.put("uuid", form.getString("uuid"));
						clientForm.put("name", form.getJsonObject("fields").getString("name"));
						clientForm.put("cms_id", form.getJsonObject("fields").getValue("cms_id"));
						clientForms.add(clientForm);
					}
					clientResponse.put("forms", new JsonArray(clientForms));

					return Single.just(clientResponse);
				});
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Update a form
	 * @param projectName project name
	 * @param uuid uuid of the form
	 * @param formData form data
	 * @return single of updated form node
	 */
	public Single<NodeResponse> update(String projectName, String uuid, JsonObject formData) {
		return Single.zip(load(projectName, uuid).flatMap(nodeResponse -> {
			Optional<ValidationError> error = checkForm(formData);

			if (error.isPresent()) {
				return Single.error(
					new FormsError(HttpResponseStatus.BAD_REQUEST.code(), error.get().getMessage()));
			}

			NodeUpdateRequest request = new NodeUpdateRequest();
			request.setLanguage("en");
			request.setVersion(nodeResponse.getVersion());
			request.getFields().put("name", new StringFieldImpl().setString(formData.getString("name")));
			request.getFields().put("json", new StringFieldImpl().setString(formData.encodePrettily()));

			String pageIdString = formData.getString("pageId", null);
			if (pageIdString != null) {
				try {
					int pageId = Integer.parseInt(pageIdString);
					request.getFields().put("pageId", new NumberFieldImpl().setNumber(pageId));
				} catch (NumberFormatException e) {
					log.error(String.format("Error while saving form: %s is not a valid pageId", pageIdString));
				}
			}

			return userClient.updateNode(projectName, uuid, request).toSingle();
		}), schemaService.loadSchemaByName(SchemaService.getSchemaName(uuid), true), (formNode, schema) -> {
			// update the schema
			SchemaUpdateRequest updateRequest = getSchema(formNode, new SchemaUpdateRequest());
			// The explicit cast is necessary to prevent an Eclipse internal error and can probably be removed in the future.
			Single<NodeResponse> r = userClient.updateSchema(schema.get().getUuid(), updateRequest).toSingle().map(updatedSchema -> formNode);
			return r;
		}).flatMap(x -> x).flatMap(formNode -> {
			return userClient.publishNode(projectName, formNode.getUuid()).toSingle().map(publishStatus -> formNode);
		});
	}

	/**
	 * Insert or update the form
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param formData form data
	 * @return single node response
	 */
	public Single<NodeResponse> upsert(String projectName, String uuid, JsonObject formData) {
		Optional<ValidationError> error = checkForm(formData);

		if (error.isPresent()) {
			return Single.error(
				new FormsError(HttpResponseStatus.BAD_REQUEST.code(), error.get().getMessage()));
		}

		String language = formData.getString("language", "en");
		NodeUpsertRequest request = new NodeUpsertRequest()
				.setSchemaName(SchemaInitializer.SCHEMA_NAME)
				.setParentNodeUuid(schemaService.getRootNodeUuid(projectName))
				.setLanguage(language);

		request.getFields().put("name", new StringFieldImpl().setString(formData.getString("name")));
		request.getFields().put("json", new StringFieldImpl().setString(formData.encodePrettily()));
		Object cmsIdObject = formData.getValue("cms_id", null);
		if (cmsIdObject != null) {
			try {
				int cmsId = Integer.parseInt(cmsIdObject.toString());
				request.getFields().put("cms_id", new NumberFieldImpl().setNumber(cmsId));
			} catch (NumberFormatException e) {
				log.error(String.format("Error while saving form: %s is not a valid cmsId", cmsIdObject));
			}
		}

		return userClient.upsertNode(projectName, uuid, request, new NodeParametersImpl().setLanguages(language)).toSingle().flatMap(formNode -> {
			return schemaService.loadSchemaByName(SchemaService.getSchemaName(uuid), false).flatMap(optSchema -> {
				if (optSchema.isPresent()) {
					log.info("Found schema {}. Schema has uuid {}", SchemaService.getSchemaName(uuid), optSchema.get().getUuid());
					// update schema
					return updateSchema(projectName, formNode, optSchema.get()).map(response -> formNode);
				} else {
					log.info("Did not find schema {}.", SchemaService.getSchemaName(uuid));
					// create schema
					return createAndAssignSchema(projectName, formNode).map(schemaResponse -> formNode);
				}
			});
		}).flatMap(updatedForm -> {
			return userClient.publishNode(projectName, updatedForm.getUuid()).toSingle().flatMap(r -> load(projectName, updatedForm.getUuid()));
		});
	}

	/**
	 * Deletes the specified form (and thus also its subnodes/data) and its schema.
	 *
	 * @param projectName project name
	 * @param formUuid
	 *            the UUID of the form to be deleted
	 */
	public Single<EmptyResponse> delete(String projectName, String formUuid) {
		Single<NodeResponse> formNode = load(projectName, formUuid);
		Single<Optional<SchemaResponse>> schema = schemaService.loadSchemaByName(getSchemaName(formUuid), true);
		return formNode.flatMap(formNodeResponse -> {
			return schema.flatMap(schemaResponse -> {
				DeleteParametersImpl formDeleteParams = new DeleteParametersImpl();
				formDeleteParams.setRecursive(true);
				return userClient.deleteNode(projectName, formNodeResponse.getUuid(), formDeleteParams).toSingle().flatMap(formDeleteResponse -> {
					return userClient.deleteSchema(schemaResponse.get().getUuid()).toSingle();
				});
			});
		});
	}

	/**
	 * Gets the schema for the specified form and adds the cms_id stored in the form node to it
	 *
	 * @param projectName project name
	 * @param formUuid
	 *            the UUID of the form
	 */
	public Single<JsonObject> getFormSchemaWithCmsId(String projectName, String formUuid) {
		Single<Optional<SchemaResponse>> schema = schemaService.loadSchemaByName(getSchemaName(formUuid), true);
		return Single.zip(schema, load(projectName, formUuid), (formSchemaResponse, formNodeResponse) -> {
			NumberField cmsIdField = formNodeResponse.getFields().getNumberField("cms_id");
			JsonObject formSchema = new JsonObject(formSchemaResponse.get().toJson());
			return formSchema.put("cms_id", cmsIdField != null ? cmsIdField.getNumber() : null);
		});
	}

	/**
	 * Prepare an existing form to be rendered by the template handler
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param rc routing context
	 * @param lang optional languages
	 * @return single buffer
	 */
	public Single<Buffer> render(String projectName, String uuid, RoutingContext rc, String...lang) {
		return statusService.languageFallback(projectName, lang)
				.flatMap(langFallback -> loadAsAdmin(projectName, uuid, langFallback).flatMap(formNode -> {
					JsonObject formJson = new JsonObject(formNode.getFields().getStringField("json").getString());

					removeInactiveElements(formJson);

					// add id
					formJson.put("id", uuid);
					// add options
					formJson.put("options", JsonObject.mapFrom(configuration));
					markFormpages(formJson, Collections.emptySet());

					rc.put("form", formJson);

					return templateEngine.rxRender(rc.data(), "form");
				}));
	}

	/**
	 * Render a preview for the given formJson
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param formJson form JSON to render
	 * @param rc routing context
	 * @return single buffer
	 */
	public Single<Buffer> renderPreview(String projectName, String uuid, JsonObject formJson, RoutingContext rc) {
		removeInactiveElements(formJson);

		// add id
		formJson.put("id", uuid);
		// add options
		formJson.put("options", JsonObject.mapFrom(configuration));
		markFormpages(formJson, Collections.emptySet());

		rc.put("form", formJson);

		return templateEngine.rxRender(rc.data(), "form");
	}

	/**
	 * Render a preview for the given formJson and return as JSON
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param formJson form JSON to render
	 * @param rc routing context
	 * @return single buffer
	 */
	public Single<RenderResponse> renderPreviewAsJson(String projectName, String uuid, JsonObject formJson, RoutingContext rc) {
		removeInactiveElements(formJson);

		// add id
		formJson.put("id", uuid);
		// add options
		formJson.put("options", JsonObject.mapFrom(configuration));
		markFormpages(formJson, Collections.emptySet());

		rc.put("form", formJson);

		return templateEngine.rxRender(rc.data(), "form").map(buffer -> new RenderResponse().setHtml(buffer.toString()));
	}

	/**
	 * Mark completed and active formpages
	 *
	 * @param form
	 *            form
	 * @param completed
	 *            names of completed formpages
	 */
	public void markFormpages(JsonObject form, Set<String> completed) {
		JsonArray elements = form.getJsonArray("elements");
		if (elements != null) {
			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.getJsonObject(i);
				if (!"formpage".equals(element.getString("type"))) {
					continue;
				}
				String name = element.getString("name");
				if (completed.contains(name)) {
					element.put("class", "is-completed");
				} else {
					element.put("class", "is-active");
					element.put("active", true);
					break;
				}
			}
		}
	}

	/**
	 * Remove inactive elements from the form
	 * @param form form, which will be modified
	 */
	public void removeInactiveElements(JsonObject form) {
		JsonArray elements = form.getJsonArray("elements");
		if (elements != null) {
			form.put("elements", removeInactiveElements(elements));
		}
	}

	/**
	 * Transform the form node into a form object
	 *
	 * @param formNode
	 *            form node
	 * @return single form
	 */
	public Single<Form> toForm(NodeResponse formNode) {
		try {

			return Single.just(toForm(formNode.getFields().getStringField("json").toString(), formNode.getUuid()));
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Transfrom the string with the JSON representation of a form to a {@link Form} object.
	 * @param formData The JSON representation of a <code>Form</code>
	 * @param formUuid The UUID of the given form
	 * @return The deserialized <code>Form</code> object
	 * @throws IOException When the object mapper cannot parse the string
	 */
	private Form toForm(String formData, String formUuid) throws IOException {
		Form form = objectMapper.readValue(formData, Form.class);

		form.setUuid(formUuid);

		return form;
	}

	/**
	 * Remove inactive elements from the elements array
	 * @param elements elements
	 * @return cleaned elements array
	 */
	private JsonArray removeInactiveElements(JsonArray elements) {
		JsonArray cleanedElements = new JsonArray();
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.getJsonObject(i);
			if (element.getBoolean("active", true)) {
				cleanedElements.add(element);
				JsonArray subElements = element.getJsonArray("elements");
				if (subElements != null) {
					element.put("elements", removeInactiveElements(subElements));
				}
			}
		}
		return cleanedElements;
	}

	/**
	 * Get the schema name for the form uuid
	 *
	 * @param formUuid
	 *            form uuid
	 * @return schema name
	 */
	private String getSchemaName(String formUuid) {
		return String.format("%s_%s", SchemaService.SCHEMA_NAME, formUuid);
	}

	/**
	 * Transform the form data stored in the form node (in attribute "json") into a schema
	 *
	 * @param formNode
	 *            form node
	 * @param schema
	 *            schema
	 * @return schema filled with data
	 * @throws FormsError
	 */
	private <T extends Schema> T getSchema(NodeResponse formNode, T schema) throws FormsError {
		schema.setName(getSchemaName(formNode.getUuid()));
		schema.setContainer(false);
		try {
			Form form = objectMapper.readValue(formNode.getFields().getStringField("json").toString(), Form.class);
			form.addFieldSchemas(schema);
		} catch (IOException e) {
			throw new FormsError(e);
		}

		// add field for errors
		schema.addField(new StringFieldSchemaImpl().setName(Form.ERRORS_FIELD_NAME).setLabel("Errors"));

		return schema;
	}

	/**
	 * Check whether the node is a form node (uses the form schema)
	 *
	 * @param node
	 *            node to check
	 * @return true for form nodes
	 */
	private boolean isFormNode(NodeResponse node) {
		return SchemaService.SCHEMA_NAME.equals(node.getSchema().getName());
	}

	private Single<SchemaResponse> createAndAssignSchema(String projectName, NodeResponse formNode) throws FormsError {
		SchemaCreateRequest schema = getSchema(formNode, new SchemaCreateRequest());
		log.info("Creating schema {}", schema.getName());
		return userClient.createSchema(schema).toSingle().flatMap(createdSchema -> {
			return userClient.assignSchemaToProject(projectName, createdSchema.getUuid()).toSingle();
		});
	}

	private Single<GenericMessageResponse> updateSchema(String projectName, NodeResponse formNode, SchemaResponse schema) throws FormsError {
		log.info("Updating schema {} with uuid {}", schema.getName(), schema.getUuid());
		SchemaUpdateRequest updateRequest = getSchema(formNode, new SchemaUpdateRequest());
		return  userClient.updateSchema(schema.getUuid(), updateRequest).toSingle();
	}
}
