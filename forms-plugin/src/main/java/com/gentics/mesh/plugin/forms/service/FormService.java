package com.gentics.mesh.plugin.forms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.service.RenderService;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.MultipageRenderMode;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormType;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.model.response.RenderResponse;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.FileSystem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class FormService {

	/**
	 * Wrapper for paged form data for use as a state in {@link Flowable#generate(Callable, BiConsumer)}.
	 */
	private static class AvailableFormData {

		/** The last loaded batch. */
		private final Deque<NodeResponse> availableData = new ArrayDeque<>(CHILDREN_BATCH_SIZE);
		/** The number of pages. When this is &lt; 0, no batch has been loaded yet. */
		private long pageCount = -1;
		/** The next page to load. */
		private long nextPage = 1;

		/**
		 * Whether a request to load the next page is needed.
		 *
		 * <p>
		 *     This returns true, when no request was sent yet (i.e. {@link #pageCount}
		 *     is less than zero), or there is no more data available and we
		 *     still have not loaded the last page yet.
		 * </p>
		 * @return
		 */
		public boolean needUpdate() {
			return pageCount == -1 || (nextPage <= pageCount && availableData.isEmpty());
		}

		/**
		 * Update the state with the information from the given response.
		 *
		 * @param response The last batch of loaded form data nodes.
		 */
		public void update(NodeListResponse response) {
			if (!availableData.isEmpty()) {
				log.warn("Available form data state was updated while there was still data available");
			}

			availableData.addAll(response.getData());

			PagingMetaInfo metaInfo = response.getMetainfo();

			pageCount = metaInfo.getPageCount();
			nextPage = metaInfo.getCurrentPage() + 1;
		}

		/**
		 * Get the index of the next page to load.
		 * @return The index of the next page to load
		 */
		public long nextPage() {
			return nextPage;
		}

		/**
		 * Emit the next event.
		 *
		 * <p>
		 *     When there is still data available, it will call the emitters
		 *     {@link Emitter#onNext(Object) onNext()} method}, otherwise it
		 *     will call {@link Emitter#onComplete() onComplete()}.
		 * </p>
		 *
		 * @param emitter The emitter to use
		 */
		public void next(Emitter<NodeResponse> emitter) {
			NodeResponse next = null;

			while (!availableData.isEmpty() && !FormUtil.isFormDataNode(next)) {
				next = availableData.poll();
			}

			if (next == null) {
				emitter.onComplete();
			} else {
				emitter.onNext(next);
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger(FormService.class);

	public static final String FIELD_FORM_TYPE = "type";

	private static final int CHILDREN_BATCH_SIZE = 100;

	private static final String PARAM_TEMPLATE_CONTEXT = "templateContext";
	private final String PARAM_SHOW_RESULTS = "showResults";

	private final FileSystem fileSystem;

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final ObjectMapper objectMapper;

	private final SchemaService schemaService;

	private final PagingService pagingService;

	private final RenderService renderService;

	private final FormsConfig configuration;

	private final StatusService statusService;

	@Inject
	public FormService(
			Vertx rxVertx,
			@AdminClient MeshRestClient adminClient,
			@UserClient @Nullable MeshRestClient userClient,
			ObjectMapper objectMapper,
			SchemaService schemaService,
			PagingService pagingService,
			RenderService renderService,
			FormsConfig configuration,
			StatusService statusService) {
		this.fileSystem = rxVertx.fileSystem();
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.objectMapper = objectMapper;
		this.schemaService = schemaService;
		this.pagingService = pagingService;
		this.renderService = renderService;
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
			return toForm(formData.encode(), formData.getString("uuid"), null).validateForm();
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
				return Single.error(new FormsError(message, HttpResponseStatus.NOT_FOUND.code()));
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
					return Single.error(new FormsError(message, HttpResponseStatus.NOT_FOUND.code()));
				} else {
					return Single.just(nodeResponse);
				}
			} else {
				String message = String.format("Object with uuid \"%s\" could not be found.", uuid);
				return Single.error(new FormsError(message, HttpResponseStatus.NOT_FOUND.code()));
			}
		});
	}

	/**
	 * Load the form node by uuid as admin user with the form plugin language fallback
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param published true to load published node, false for draft
	 * @param requestedLangs requested languages
	 * @return single noderesponse
	 */
	public Single<NodeResponse> loadAsAdminWithLanguageFallback(String projectName, String uuid, boolean published, String...requestedLangs) {
		return statusService.languageFallback(projectName, requestedLangs).flatMap(langs -> loadAsAdmin(projectName, uuid, published, langs));
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
	 * {
	 * 		"totalCount" : 4,
	 * 		"currentPage" : 1,
	 * 		"pageCount" : 1,
	 * 		"perPage" : 1000000,
	 * 		"forms" : [ {
	 * 			"uuid" : "29a53fee7af6468da53fee7af6568d75",
	 * 			"name" : "Formular Test 1"
	 * 		}, {
	 * 			"uuid" : "05a0c76f7d5940dfa0c76f7d5960df23",
	 * 			"name" : "Formular Nr. 2"
	 * 		}, {
	 * 			"uuid" : "5a1e034b14ef480f9e034b14efa80fe3",
	 * 			"name" : "Formular Nr. 3"
	 * 		}, {
	 * 			"uuid" : "33b96c62cb754b74b96c62cb75db74bb",
	 * 			"name" : "Formular Nr. 4"
	 * 		} ]
	 * }
	 * </pre>
	 *
	 * @param projectName project name
	 * @param pagingParams paging parameters
	 * @param requestedLanguages The languages for the forms to list. Note that the language of the Forms root node (en)
	 * 		will always be included.
	 * @return single json object
	 */
	public Single<JsonObject> list(String projectName, JsonObject pagingParams, String[] requestedLanguages) {
		try {
			String graphQlQuery = IOUtils.toString(FormsPlugin.class.getResource("forms.graphql"), StandardCharsets.UTF_8);

			Single<String[]> languages = requestedLanguages == null || requestedLanguages.length == 0
				? statusService.languageFallback(projectName)
				: Single.just(requestedLanguages);

			return languages
				.flatMap(lang -> statusService.getRootNode(projectName, userClient)
					.flatMap(rootNode -> {
						JsonObject variables = new JsonObject()
							.mergeIn(pagingParams, true)
							.put("rootNodeUuid", rootNode.getUuid())
							.put("lang", Arrays.asList(lang));

						return userClient
							.graphql(projectName, new GraphQLRequest().setQuery(graphQlQuery).setVariables(variables))
							.toSingle();
					})
					.flatMap(graphQlResponse -> {
						// log all errors
						if (graphQlResponse.getErrors() != null) {
							for (GraphQLError error : graphQlResponse.getErrors()) {
								log.warn("Error in graphql response {}", error.toJson());
							}
						}

						JsonObject clientResponse = new JsonObject();
						if (graphQlResponse.getData() == null) {
							return Single.error(new FormsError("Found no data in graphql response", 500));
						}

						if (!graphQlResponse.getData().containsKey("node")) {
							return Single.error(new FormsError("Found no 'node' in graphql response", 500));
						}
						JsonObject node = graphQlResponse.getData().getJsonObject("node");
						if (!node.containsKey("children")) {
							return Single.error(new FormsError("Found no 'node.children' in graphql response", 500));
						}
						JsonObject children = node.getJsonObject("children");
						if (!children.containsKey("elements")) {
							return Single.error(new FormsError("Found no 'node.children.elements' in graphql response", 500));
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
					}));
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
				new FormsError(error.get().getMessage(), HttpResponseStatus.BAD_REQUEST.code()));
		}

		ensureFormType(formData);

		return statusService.getRootNode(projectName, userClient).flatMap(rootNode -> {
			String language = formData.getString("language", "en");
			NodeParameters languageParameter = new NodeParametersImpl().setLanguages(language);
			NodeUpsertRequest request = new NodeUpsertRequest()
					.setSchemaName(SchemaInitializer.SCHEMA_NAME)
					.setParentNodeUuid(rootNode.getUuid())
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
	 * Make sure the  given form specification contains a valid form type.
	 *
	 * <p>
	 *     Currently only two types are supported:
	 *     <ol>
	 *         <li>
	 *             <em>GENERIC</em>: A generic form with no special restrictions
	 *             or behavior.
	 *         </li>
	 *         <li>
	 *             <em>POLL</em>: Only selection fields, read only text and
	 *             recaptchas are allowed. Requests to render the form will
	 *             instead render the results, if a cookie indicates that the
	 *             user already voted in the poll.
	 *         </li>
	 *     </ol>
	 * </p>
	 *
	 * @param formData The posted form
	 * @return The posted form with added or corrected form type field
	 */
	private JsonObject ensureFormType(JsonObject formData) {
		String rawType = formData.getString(FIELD_FORM_TYPE);

		try {
			FormType.valueOf(rawType);
		} catch (Exception e) {
			log.warn("Missing or invalid form type <{}> for form \"{}\"", rawType, formData.getString("name"));
			formData.put(FIELD_FORM_TYPE, FormType.GENERIC.name());
		}

		return formData;
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
			.flatMap(langFallback -> statusService.getPathPrefixOfDefaultBranch(projectName)
				.flatMap(pathPrefix ->loadAsAdmin(projectName, uuid, true, langFallback)
					.flatMap(this::toForm)
					.flatMap(form -> {
						if (form.getType() == FormType.POLL) {
							boolean showResults = !rc.queryParam(PARAM_SHOW_RESULTS).isEmpty()
								|| Stream.of(rc.getCookie(FormDataService.COOKIE_NAME_VOTED))
									.filter(Objects::nonNull)
									.map(c -> c.getValue().split(FormDataService.COOKIE_VOTED_SEPARATOR))
									.flatMap(Arrays::stream)
									.anyMatch(form.getUuid()::equals);

							if (showResults) {
								return renderPollResults(rc, projectName, form);
							}
						}

						return renderForm(uuid, form, pathPrefix, rc);
					})));
	}

	/**
	 * Render the form for the given {@code formJson}.
	 *
	 * @param uuid form uuid
	 * @param formData form to render encoded as JSON
	 * @param rc plugin context
	 * @return single buffer
	 */
	public Single<Buffer> renderForm(String uuid, String formData, String pathPrefix, PluginContext rc) {
		Form form;

		try {
			form = toForm(formData, uuid, ParameterUtil.getFirstLanguage(rc, "en"));
		} catch (IOException e) {
			log.error("Could not create form instance from form data: {}", e.getMessage());

			return Single.error(e);
		}
		return renderForm(uuid, form, pathPrefix, rc);
	}

	/**
	 * Render the form for the given {@code formJson}.
	 *
	 * @param uuid form uuid
	 * @param form form to render
	 * @param rc routing context
	 * @return single buffer
	 */
	public Single<Buffer> renderForm(String uuid, Form form, String pathPrefix, RoutingContext rc) {
		JsonObject formJson = form.removeInactiveElements()
			.prepareRenderContext(pathPrefix, configuration);

		markFormpages(formJson, null, Collections.emptySet());

		rc.put("form", formJson);

		return renderTemplate(rc, form.getTemplateContext(), "form");
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
		FormUtil.removeInactiveElements(formJson);

		// add id
		formJson.put("id", uuid);
		// add property "multipage"
		formJson.put("multipage", isMultipage(formJson));
		// add options
		formJson.put("options", JsonObject.mapFrom(configuration));
		markFormpages(formJson, null, Collections.emptySet());

		rc.put("form", formJson);

		return renderTemplate(rc, formJson.getString(PARAM_TEMPLATE_CONTEXT), "form")
			.map(buffer -> new RenderResponse().setHtml(buffer.toString()));
	}

	/**
	 * Mark completed and active formpages
	 *
	 * @param form
	 *            form
	 * @param completed
	 *            names of completed formpages
	 */
	public void markFormpages(JsonObject form, String currentPage, Set<String> completed) {
		JsonArray elements = form.getJsonArray("elements");
		if (elements != null) {
			boolean activePageFound = false;
			boolean forceActivePage = false;
			int count = 0;
		/*
	{{#if ../form.isMultipage}}
    <ol class="gtx-form-progress" role="progressbar" aria-valuetext="{{ ../form.currentPage.description }}"
        aria-valuemin="1" aria-valuenow="{{ ../form.currentPage.index }}" aria-valuemax="{{ ../form.pageCount }}">
    {{#each ../form.elements}}
        {{#equals type "formpage" }}
            <li class="gtx-form-progress__step {{ class }}" id="step_{{ name }}">
                <a class="c-form-progress__step-link" href="#!" data-formpageid="{{ name }}">{{{ description }}}</a>
                <div class="c-form-progress__step-description">{{ info }}</div>
            </li>
        {{/equals}}
    {{/each}}
    </ol>
{{/if}}
		 */
			JsonObject activePage = null;

			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.getJsonObject(i);
				if (!"formpage".equals(element.getString("type"))) {
					continue;
				}

				count++;

				String name = element.getString("name");

				if (completed.contains(name)) {
					element.put("class", "is-completed");
					element.put("completed", true);

					if (forceActivePage) {
						element.put("class", "is-active");
						element.put("active", true);
						activePage = new JsonObject()
							.put("index", count)
							.put("name", name)
							.put("description", element.getString("description"));
					} else {
						element.put("active", false);
					}
				} else if (forceActivePage || activePage == null) {
					element.put("class", "is-active");
					element.put("active", true);
					activePage = new JsonObject()
						.put("index", count)
						.put("name", name)
						.put("description", element.getString("description"));
				} else {
					element.put("active", false);
				}

				// When FORCE_NEXT is the multipage render mode and the current page is the one that was submitted,
				// the next one should be active, event if it is already completed.
				forceActivePage = configuration.getMultipageRenderMode() == MultipageRenderMode.FORCE_NEXT && name.equals(currentPage);
			}

			form.put("currentPage", activePage)
				.put("pageCount", count);
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

			return Single.just(toForm(formNode.getFields().getStringField("json").toString(), formNode.getUuid(), formNode.getLanguage()));
		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Transfrom the string with the JSON representation of a form to a {@link Form} object.
	 * @param formData The JSON representation of a <code>Form</code>
	 * @param formUuid The UUID of the given form
	 * @param language form language
	 * @return The deserialized <code>Form</code> object
	 * @throws IOException When the object mapper cannot parse the string
	 */
	public Form toForm(String formData, String formUuid, String language) throws IOException {
		Form form = objectMapper.readValue(formData, Form.class);

		form.setUuid(formUuid);
		if (language != null) {
			form.setLanguage(language);
		}

		return form;
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
	private <T extends SchemaModel> T getSchema(NodeResponse formNode, T schema) throws FormsError {
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
		return  userClient.updateSchema(schema.getUuid(), updateRequest).toSingle()
			.flatMap(updateResponse ->
				// Make sure the schema is assigned to the project, just in case.
				userClient.assignSchemaToProject(projectName, schema.getUuid()).toCompletable()
					.andThen(Single.just(updateResponse)));
	}

	/**
	 * Render the results for the specified poll form.
	 *
	 * @param rc The current routing context
	 * @param projectName The project name
	 * @param form The form for which results should be rendered
	 * @return A {@code Single} containing the rendered result as a {@code Buffer}
	 */
	private Single<Buffer> renderPollResults(RoutingContext rc, String projectName, Form form) {
		return getPollResults(projectName, form, form.getUuid())
			.flatMap(rs -> {
				JsonObject poll = new JsonObject()
					.put("id", form.getUuid())
					.put("options", JsonObject.mapFrom(configuration))
					.put("result", rs);

				rc.put("poll", poll);

				return renderTemplate(rc, form.getTemplateContext(), configuration.getPollOptions().getResultTemplate());
			});
	}

	/**
	 * Load the results for the specified poll form.
	 *
	 * @param projectName The project name
	 * @param form The poll form
	 * @param formUuid The UUID of the form node
	 * @return A {@code Single} containing the results in a JSON array
	 */
	private Single<JsonArray> getPollResults(String projectName, Form form, String formUuid) {
		JsonObject results = new JsonObject();

		return loadFormData(projectName, formUuid)
			.doOnNext(formData -> form.gatherPollResults(results, formData))
			.ignoreElements()
			.andThen(Single.just(results))
			.map(rs -> rs.stream()
				.filter(entry -> entry.getValue() instanceof JsonObject)
				.map(entry -> ((JsonObject) entry.getValue()).put("name", entry.getKey()))
				.collect(Collector.of(JsonArray::new, (a, o) -> a.add(o), (u, v) -> u.addAll(v))));
	}

	/**
	 * Load the next batch of children for the given form UUID.
	 *
	 * @param project The project name
	 * @param parentUuid The UUID of the parent node (a form)
	 * @return A {@code Flowable} containing data nodes for the given parent
	 */
	private Flowable<NodeResponse> loadFormData(String project, String parentUuid) {
		VersioningParameters published = new VersioningParametersImpl().published();
		PagingParameters paging = new PagingParametersImpl().setPerPage((long) CHILDREN_BATCH_SIZE);

		// get the languages of the project
		// this will be used with the $project/nodes/$uuid/children endpoint to provide the 'lang' parameter
		// the values for this parameter need to contain:
		//   - the actual language of the parent node
		//   - the default language of the mesh instance (default 'en') as data nodes will be created in this language
		return statusService.getLanguages(project, adminClient)
			.map(availableLanguages -> {
				List<String> languagesForRequest = new ArrayList<>();
				if (availableLanguages.getLanguages() != null) {
					languagesForRequest.addAll(availableLanguages.getLanguages());
				}
				// if the available languages in the project do not contain english, this will result in no data being loaded
				// so we make sure to always add 'en'
				if (!languagesForRequest.contains("en")) {
					languagesForRequest.add("en");
				}
				return languagesForRequest.toArray(new String[languagesForRequest.size()]);
			})
			.map(availableLanguages -> new NodeParametersImpl().setLanguages(availableLanguages))
			.toFlowable()
			.flatMap(lang -> Flowable.generate(AvailableFormData::new, (state, emitter) -> {
				if (state.needUpdate()) {
					NodeListResponse nextBatch = adminClient.findNodeChildren(project, parentUuid, paging.setPage(state.nextPage()), published, lang).toSingle()
						.doOnSubscribe(ignored -> log.info("Loading next batch of form data nodes (max: {})", CHILDREN_BATCH_SIZE))
						// TODO: Find a way to only load the next batch, once it is needed without blocking.
						.blockingGet();

					state.update(nextBatch);
				}

				state.next(emitter);
			}));
	}

	/**
	 * Find the json object representing the element with given name
	 *
	 * @param form
	 *            form to search
	 * @param name
	 *            element name
	 * @return json object or null if not found
	 */
	public JsonObject findElement(JsonObject form, String name) {
		if (StringUtils.isBlank(name) || !form.containsKey("elements")) {
			return null;
		}

		JsonArray elements = form.getJsonArray("elements");

		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.getJsonObject(i);

			if (name.equals(element.getString("name"))) {
				return element;
			}

			JsonObject subElement = findElement(element, name);

			if (subElement != null) {
				return subElement;
			}
		}

		return null;
	}

	/**
	 * Render the given template with the given context.
	 *
	 * @param rc The current routing context
	 * @param templateContext The forms template context
	 * @param templateName The name of the template to be rendered (may contain path segments)
	 * @return A {@code Single} containing the rendered template
	 */
	private Single<Buffer> renderTemplate(RoutingContext rc, String templateContext, String templateName) {
		String basePath = configuration.getTemplatesPath();
		// Observables cannot handle null values, so we filter those first using Java streams.
		List<String> validContexts = Stream.of(
				rc.queryParam(PARAM_TEMPLATE_CONTEXT).stream().filter(StringUtils::isNotBlank).findFirst().orElse(null),
				templateContext)
			.filter(StringUtils::isNotBlank)
			.collect(Collectors.toList());

		return Observable.fromIterable(validContexts)
			.map(ctx -> String.format("%s/%s/%s.hbs", basePath, ctx, templateName))
			.flatMap(path -> fileSystem.rxExists(path)
				.flatMapObservable(exists -> {
					if (exists) {
						return Observable.just(path);
					}

					log.debug("Template <{}> not found", path);

					return Observable.empty();
				}))
			.first(String.format("%s/%s.hbs", basePath, templateName))
			.doOnSuccess(template -> log.debug("Using template <{}>", template))
			.flatMap(template -> renderService.rxRender(rc.data(), StringUtils.removeEnd(template, ".hbs")));
	}
}
