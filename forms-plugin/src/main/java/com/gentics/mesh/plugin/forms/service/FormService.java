package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
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
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.model.response.RenderResponse;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;
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

//	/**
//	 * Load the form node by uuid
//	 *
//	 * @param projectName project name
//	 * @param uuid
//	 *            uuid
//	 * @return single noderesponse
//	 */
//	public Single<NodeResponse> load(String projectName, String uuid) {
//		return userClient.findNodeByUuid(projectName, uuid).toSingle().flatMap(nodeResponse -> {
//			if (isFormNode(nodeResponse)) {
//				return Single.just(nodeResponse);
//			} else {
//				String message = String.format("Object with uuid \"%s\" could not be found.", uuid);
//				return Single.error(new FormsError(HttpResponseStatus.NOT_FOUND.code(), message));
//			}
//		});
//	}

	/**
	 * Load the form node by uuid in the given language
	 *
	 * @param projectName project name
	 * @param uuid uuid
	 * @param lang optional languages
	 * @return single noderesponse
	 */
	public Single<NodeResponse> load(String projectName, String uuid, String...lang) {
		NodeParameters nParams = new NodeParametersImpl();
		if (lang.length > 0) {
			nParams.setLanguages(lang);
		}
		return userClient.findNodeByUuid(projectName, uuid, nParams).toSingle().flatMap(nodeResponse -> {
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
	 * @param published true to load published node, false for draft
	 * @param lang optional languages
	 * @return single noderesponse
	 */
	public Single<NodeResponse> loadAsAdmin(String projectName, String uuid, boolean published, String...lang) {
		NodeParameters nParams = new NodeParametersImpl();
		if (lang.length > 0) {
			nParams.setLanguages(lang);
		}
		VersioningParameters vParams = new VersioningParametersImpl().setVersion(published ? "published" : "draft");
		return adminClient.findNodeByUuid(projectName, uuid, nParams, vParams).toSingle().flatMap(nodeResponse -> {
			if (isFormNode(nodeResponse)) {
				if (published && !FormOnlineStatusService.isOnline(nodeResponse)) {
					String message = String.format("Form with uuid \"%s\" is not online.", uuid);
					return Single.error(new FormsError(HttpResponseStatus.NOT_FOUND.code(), message));
				} else {
					return Single.just(nodeResponse);
				}
			} else {
				String message = String.format("Object with uuid \"%s\" could not be found.", uuid);
				return Single.error(new FormsError(HttpResponseStatus.NOT_FOUND.code(), message));
			}
		});
	}

	/**
	 * Load the form node by uuid as admin user with the form plugin language fallback
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param published true to load published node, false for draft
	 * @return single noderesponse
	 */
	public Single<NodeResponse> loadAsAdminWithLanguageFallback(String projectName, String uuid, boolean published) {
		return statusService.languageFallback(projectName).flatMap(langs -> loadAsAdmin(projectName, uuid, published, langs));
	}

	/**
	 * Get the elements JSON for the form
	 * @param projectName project name
	 * @param uuid uuid
	 * @param lang optional languages
	 * @return single JsonArray containing the elements
	 */
	public Single<JsonObject> getElements(String projectName, String uuid, String...lang) {
		return statusService.languageFallback(projectName, lang)
				.flatMap(fallback -> loadAsAdmin(projectName, uuid, true, fallback).flatMap(formNode -> {
					JsonObject formJson = new JsonObject(formNode.getFields().getStringField("json").getString());
					return Single.just(new JsonObject().put("elements", formJson.getJsonArray("elements")));
				}));
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
				 "name" : "Formular Test 1"
			 }, {
				 "uuid" : "05a0c76f7d5940dfa0c76f7d5960df23",
				 "name" : "Formular Nr. 2"
			 }, {
				 "uuid" : "5a1e034b14ef480f9e034b14efa80fe3",
				 "name" : "Formular Nr. 3"
			 }, {
				 "uuid" : "33b96c62cb754b74b96c62cb75db74bb",
				 "name" : "Formular Nr. 4"
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

			return statusService.getRootNode(projectName, userClient)
					.flatMap(rootNode -> {
						JsonObject variables = new JsonObject().mergeIn(pagingParams, true).put("rootNodeUuid", rootNode.getUuid());
						return userClient
								.graphql(projectName,
										new GraphQLRequest().setQuery(graphQlQuery).setVariables(variables))
								.toSingle();
					})				.flatMap(graphQlResponse -> {
					// log all errors
					if (graphQlResponse.getErrors() != null) {
						for (GraphQLError error : graphQlResponse.getErrors()) {
							log.warn("Error in graphql response {}", error.toJson());
						}
					}

					JsonObject clientResponse = new JsonObject();
					if (graphQlResponse.getData() == null) {
						return Single.error(new FormsError(500, "Found no data in graphql response"));
					}

					if (!graphQlResponse.getData().containsKey("node")) {
						return Single.error(new FormsError(500, "Found no 'node' in graphql response"));
					}
					JsonObject node = graphQlResponse.getData().getJsonObject("node");
					if (!node.containsKey("children")) {
						return Single.error(new FormsError(500, "Found no 'node.children' in graphql response"));
					}
					JsonObject children = node.getJsonObject("children");
					if (!children.containsKey("elements")) {
						return Single.error(new FormsError(500, "Found no 'node.children.elements' in graphql response"));
					}
					JsonArray forms = children.getJsonArray("elements");
					pagingService.addPagingInfoToResponse(clientResponse, children);

					// make response flat
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

		return statusService.getRootNode(projectName, userClient).flatMap(rootNodeUuid -> {
			String language = formData.getString("language", "en");
			NodeParameters languageParameter = new NodeParametersImpl().setLanguages(language);
			NodeUpsertRequest request = new NodeUpsertRequest()
					.setSchemaName(SchemaInitializer.SCHEMA_NAME)
					.setParentNodeUuid(rootNodeUuid.getUuid())
					.setLanguage(language);

			request.getFields().put("name", new StringFieldImpl().setString(formData.getString("name")));
			request.getFields().put("json", new StringFieldImpl().setString(formData.encodePrettily()));
			// always set the form "online"
			request.getFields().put(FormOnlineStatusService.FIELD_NAME, new BooleanFieldImpl().setValue(true));
			Object cmsIdObject = formData.getValue("cms_id", null);
			if (cmsIdObject != null) {
				try {
					int cmsId = Integer.parseInt(cmsIdObject.toString());
					request.getFields().put("cms_id", new NumberFieldImpl().setNumber(cmsId));
				} catch (NumberFormatException e) {
					log.error(String.format("Error while saving form: %s is not a valid cmsId", cmsIdObject));
				}
			}

			return userClient.upsertNode(projectName, uuid, request, languageParameter)
					.toSingle() /* .flatMap(formNode -> loadAsAdminWithLanguageFallback(projectName, formNode.getUuid(), false)) */
					.flatMap(formNode -> {
						return schemaService.loadSchemaByName(SchemaService.getSchemaName(uuid), false)
								.flatMap(optSchema -> {
									if (optSchema.isPresent()) {
										log.info("Found schema {}. Schema has uuid {}", SchemaService.getSchemaName(uuid),
												optSchema.get().getUuid());
										// update schema
										return updateSchema(projectName, formNode, optSchema.get())
												.map(response -> formNode);
									} else {
										log.info("Did not find schema {}.", SchemaService.getSchemaName(uuid));
										// create schema
										return createAndAssignSchema(projectName, formNode).map(schemaResponse -> formNode);
									}
								});
					}).flatMap(updatedForm -> {
						return userClient.publishNode(projectName, updatedForm.getUuid(), languageParameter).toSingle()
								.flatMap(r -> load(projectName, updatedForm.getUuid(), language));
					});
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

	public Single<JsonObject> loadForm(String projectName, String uuid, String... lang) {
		return statusService.languageFallback(projectName, lang)
				.flatMap(langFallback -> load(projectName, uuid, langFallback)).map(formNode -> {
					// TODO if the node has no fields, no specified language was found
					return new JsonObject(formNode.getFields().getStringField("json").getString());
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
				.flatMap(langFallback -> loadAsAdmin(projectName, uuid, true, langFallback).flatMap(formNode -> {
					JsonObject formJson = new JsonObject(formNode.getFields().getStringField("json").getString());

					removeInactiveElements(formJson);

					// add id
					formJson.put("id", uuid);
					// add property "multipage"
					formJson.put("multipage", isMultipage(formJson));
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
		// add property "multipage"
		formJson.put("multipage", isMultipage(formJson));
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
		// add property "multipage"
		formJson.put("multipage", isMultipage(formJson));
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
			boolean activePageFound = false;
			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.getJsonObject(i);
				if (!"formpage".equals(element.getString("type"))) {
					continue;
				}
				String name = element.getString("name");
				if (completed.contains(name)) {
					element.put("class", "is-completed");
					element.put("active", false);
				} else if (!activePageFound) {
					element.put("class", "is-active");
					element.put("active", true);
					activePageFound = true;
				} else {
					element.put("active", false);
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
	 * Check whether the form is "multipage" (i.e. contains at least one element of type "formpage")
	 * @param form form
	 * @return true if the form is "multipage"
	 */
	public boolean isMultipage(JsonObject form) {
		JsonArray elements = form.getJsonArray("elements");
		if (elements != null) {
			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.getJsonObject(i);
				if (Objects.equals("formpage", element.getString("type"))) {
					return true;
				}
			}
		}
		return false;
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
			if (ParameterUtil.asBoolean(element.getValue("active"), true)) {
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
