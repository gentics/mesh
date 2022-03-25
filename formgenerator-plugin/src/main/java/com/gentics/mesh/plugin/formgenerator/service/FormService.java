package com.gentics.mesh.plugin.formgenerator.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.formgenerator.FormGeneratorError;
import com.gentics.mesh.plugin.formgenerator.FormGeneratorPlugin;
import com.gentics.mesh.plugin.formgenerator.SchemaInitializer;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.model.Form;
import com.gentics.mesh.plugin.formgenerator.model.ValidationError;
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

	private final ObjectMapper objectMapper;

	private final SchemaService schemaService;

	private final PagingService pagingService;

	private final HandlebarsTemplateEngine templateEngine;

	private final FormGeneratorConfig configuration;

	@Inject
	public FormService(@AdminClient MeshRestClient adminClient, ObjectMapper objectMapper, SchemaService schemaService, PagingService pagingService,
					   HandlebarsTemplateEngine templateEngine, FormGeneratorConfig configuration) {
		this.adminClient = adminClient;
		this.objectMapper = objectMapper;
		this.schemaService = schemaService;
		this.pagingService = pagingService;
		this.templateEngine = templateEngine;
		this.configuration = configuration;
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
				new FormGeneratorError(HttpResponseStatus.BAD_REQUEST.code(), error.get().getMessage()));
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
		return adminClient.createNode(projectName, request).toSingle().flatMap(createdFormNode -> {
			SchemaCreateRequest schema = getSchema(createdFormNode, new SchemaCreateRequest());
			return adminClient.createSchema(schema).toSingle().flatMap(createdSchema -> {
				return adminClient.assignSchemaToProject(projectName, createdSchema.getUuid()).toSingle().map(r -> createdFormNode);
			});
		}).flatMap(createdFormNode -> {
			return adminClient.publishNode(projectName, createdFormNode.getUuid()).toSingle().map(r -> createdFormNode);
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
		return adminClient.findNodeByUuid(projectName, uuid).toSingle().flatMap(nodeResponse -> {
			if (isFormNode(nodeResponse)) {
				return Single.just(nodeResponse);
			} else {
				String message = String.format("Object with uuid \"%s\" could not be found.", uuid);
				return Single.error(new FormGeneratorError(HttpResponseStatus.NOT_FOUND.code(), message));
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
			String graphQlQuery = IOUtils.toString(FormGeneratorPlugin.class.getResource("forms.graphql"), StandardCharsets.UTF_8);

			return adminClient.graphql(projectName, new GraphQLRequest().setQuery(graphQlQuery).setVariables(pagingParams)).toSingle()
				.flatMap(graphQlResponse -> {
					JsonObject clientResponse = new JsonObject();
					JsonObject nodes = graphQlResponse.getData().getJsonObject("schema").getJsonObject("nodes");
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
						clientForm.put("pageId", form.getJsonObject("fields").getValue("pageId"));
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
					new FormGeneratorError(HttpResponseStatus.BAD_REQUEST.code(), error.get().getMessage()));
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

			return adminClient.updateNode(projectName, uuid, request).toSingle();
		}), schemaService.loadSchemaByName(SchemaService.getSchemaName(uuid), true), (formNode, schema) -> {
			// update the schema
			SchemaUpdateRequest updateRequest = getSchema(formNode, new SchemaUpdateRequest());
			// The explicit cast is necessary to prevent an Eclipse internal error and can probably be removed in the future.
			Single<NodeResponse> r = adminClient.updateSchema(schema.get().getUuid(), updateRequest).toSingle().map(updatedSchema -> formNode);
			return r;
		}).flatMap(x -> x).flatMap(formNode -> {
			return adminClient.publishNode(projectName, formNode.getUuid()).toSingle().map(publishStatus -> formNode);
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
				return adminClient.deleteNode(projectName, formNodeResponse.getUuid(), formDeleteParams).toSingle().flatMap(formDeleteResponse -> {
					return adminClient.deleteSchema(schemaResponse.get().getUuid()).toSingle();
				});
			});
		});
	}

	/**
	 * Gets the schema for the specified form and adds the pageId stored in the form node to it
	 *
	 * @param projectName project name
	 * @param formUuid
	 *            the UUID of the form
	 */
	public Single<JsonObject> getFormSchemaWithPageId(String projectName, String formUuid) {
		Single<Optional<SchemaResponse>> schema = schemaService.loadSchemaByName(getSchemaName(formUuid), true);
		return Single.zip(schema, load(projectName, formUuid), (formSchemaResponse, formNodeResponse) -> {
			NumberField pageIdField = formNodeResponse.getFields().getNumberField("pageId");
			JsonObject formSchema = new JsonObject(formSchemaResponse.get().toJson());
			return formSchema.put("pageId", pageIdField != null ? pageIdField.getNumber() : null);
		});
	}

	/**
	 * Prepare an existing form to be rendered by the template handler
	 *
	 */
	public Single<Buffer> render(String projectName, String uuid, RoutingContext rc) {
		return load(projectName, uuid).flatMap(formNode -> {
			JsonObject formJson = new JsonObject(formNode.getFields().getStringField("json").getString());

			removeInactiveElements(formJson);

			// add id
			formJson.put("id", uuid);
			// add options
			formJson.put("options", JsonObject.mapFrom(configuration));
			markFormpages(formJson, Collections.emptySet());

			rc.put("form", formJson);

			return templateEngine.rxRender(rc.data(), "form");
		});
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
	 * @throws FormGeneratorError
	 */
	private <T extends SchemaModel> T getSchema(NodeResponse formNode, T schema) throws FormGeneratorError {
		schema.setName(getSchemaName(formNode.getUuid()));
		schema.setContainer(false);
		try {
			Form form = objectMapper.readValue(formNode.getFields().getStringField("json").toString(), Form.class);
			form.addFieldSchemas(schema);
		} catch (IOException e) {
			throw new FormGeneratorError(e);
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
}
