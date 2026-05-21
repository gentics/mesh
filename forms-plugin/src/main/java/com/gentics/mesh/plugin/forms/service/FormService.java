package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
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
import com.gentics.mesh.plugin.forms.model.AdminMailElement;
import com.gentics.mesh.plugin.forms.model.BinaryElement;
import com.gentics.mesh.plugin.forms.model.CaptchaConfigModel;
import com.gentics.mesh.plugin.forms.model.DefaultValue;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.model.ValueListItem;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.FormDataProperty;
import com.gentics.mesh.plugin.forms.model.schema.PossibleValue;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;

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
	public static final String DOWNLOADS_FOLDER_NAME = "Downloads";

	public static final String DATA_FOLDER_NAME = "Data";

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

	private final String formsGraphQlQuery;

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

		try {
			this.formsGraphQlQuery = IOUtils.toString(FormsPlugin.class.getResource("forms.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		try {
			// first try to deserialize the form
			Form form = toForm(formData.encode(), formData.getString("uuid"), null);

			if (!configuration.isValidateElements()) {
				// no validation, so return empty optional
				return Optional.empty();
			} else {
				// validate and return the result
				return form.validateForm();
			}
		} catch (IOException | FormsError e) {
			log.error("Could not deserialize form: {}", e, e.getMessage());

			if (log.isTraceEnabled()) {
				log.trace("Received formData:\n{}", formData.encodePrettily());

			}
			ValidationError error = new ValidationError()
					.setName(formData.encode())
					.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.form.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.form.bad_request"));

			return Optional.of(error);
		}
	}

	/**
	 * Load the form node as User by uuid in the given language
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
	 * Load the form node as admin by uuid in the given language
	 *
	 * @param projectName project name
	 * @param uuid uuid
	 * @param lang optional languages
	 * @return single noderesponse
	 */
	public Single<NodeResponse> loadFormAsAdmin(String projectName, String uuid, String...lang) {
		NodeParameters nParams = new NodeParametersImpl();
		if (lang.length > 0) {
			nParams.setLanguages(lang);
		}
		return adminClient.findNodeByUuid(projectName, uuid, nParams).toSingle().flatMap(nodeResponse -> {
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
					try {
						Form form = toForm(formNode.getFields().getStringField("json").getString(),
								formNode.getUuid(), formNode.getLanguage());
						JsonArray elements = new JsonArray(form.getSchema().getProperties().values().stream()
								.map(e -> new JsonObject(e.toJson()))
								.collect(Collectors.toList()));
						return Single.just(new JsonObject().put("elements", elements));
					} catch (IOException e) {
						return Single.error(e);
					}
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
						.graphql(projectName, new GraphQLRequest().setQuery(formsGraphQlQuery).setVariables(variables))
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
	}

	/**
	 * List all form UUIDs for the given project
	 * @param projectName project name
	 * @return observable emitting the form UUIDs
	 */
	public Observable<String> listUuids(String projectName) {
		return statusService.languageFallback(projectName)
			.flatMap(lang -> statusService.getRootNode(projectName, adminClient).flatMap(rootNode -> {
				JsonObject variables = new JsonObject().put("rootNodeUuid", rootNode.getUuid()).put("lang",
						Arrays.asList(lang));

				return adminClient.graphql(projectName,
						new GraphQLRequest().setQuery(formsGraphQlQuery).setVariables(variables)).toSingle();
			})).flatMapObservable(graphQlResponse -> {
				List<String> uuids = FormUtil.getFromResponse(graphQlResponse, "$.data.node.children.elements[*].uuid");
				return Observable.fromIterable(uuids);
			}).doOnSubscribe(disp -> {
				log.debug(String.format("List Form uuids for project %s", projectName));
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
				new FormsError(error.toString(), HttpResponseStatus.BAD_REQUEST.code()));
		}

		return statusService.getRootNode(projectName, userClient).flatMap(rootNode -> {
			String language = formData.getString("language", "en");
			NodeUpsertRequest request = new NodeUpsertRequest()
					.setSchemaName(SchemaInitializer.SCHEMA_NAME)
					.setParentNodeUuid(rootNode.getUuid())
					.setLanguage("en");

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
					log.error(String.format("Error while saving form: %s is not a valid cmsId", cmsIdObject), e);
				}
			}

			return userClient.upsertNode(projectName, uuid, request)
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
										log.info("Did not find schema {}, creating one.", SchemaService.getSchemaName(uuid));
										// create schema
										return createAndAssignSchema(projectName, formNode).map(schemaResponse -> formNode);
									}
								});
					}).flatMap(updatedForm -> {
						return userClient.publishNode(projectName, updatedForm.getUuid()).toSingle()
								.flatMap(r -> load(projectName, updatedForm.getUuid(), language));
					}).flatMap(updatedForm -> {
						return ensureFormFolders(projectName, updatedForm.getUuid(), language, DOWNLOADS_FOLDER_NAME, DATA_FOLDER_NAME)
							.andThen(Single.just(updatedForm));
					});
		});
	}

	/**
	 * Get the UUID of the form folder node with the name {@code folderName}
	 * which is a child of the node with UIUD {@code formUuid}.
	 *
	 * @param projectName The project name.
	 * @param formUuid The form UUID to load a form folder for.
	 * @param lang The requested language.
	 * @param folderName The name of the form folder.
	 * @return A {@code Single} with the UUID of the specified folder.
	 */
	public Single<String> getFormFolderUuid(String projectName, String formUuid, String[] lang, String folderName) {
		if (StringUtils.isBlank(folderName)) {
			return Single.error(new Exception("Folder name must not be empty"));
		}

		String query;

		try {
			query = IOUtils.toString(FormsPlugin.class.getResource("findFormFolders.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return Single.error(e);
		}

		GraphQLRequest request = new GraphQLRequest()
			.setQuery(query)
			.setVariables(new JsonObject().put("formUuid", formUuid).put("lang", Arrays.asList(lang)));

		return loadAsAdminWithLanguageFallback(projectName, formUuid, false, lang).flatMap(form -> {
			return adminClient.graphql(projectName, request).toSingle()
					.map(rs -> Optional.ofNullable(rs.getData())
							.map(data -> data.getJsonObject("node"))
							.map(node -> node.getJsonObject("children"))
							.map(children -> children.getJsonArray("elements"))
							.orElse(new JsonArray())
							.stream()
							.filter(o -> o instanceof JsonObject && folderName.equals(((JsonObject) o).getString("displayName")))
							.map(o -> ((JsonObject) o).getString("uuid"))
							.findFirst())
					.flatMap(folderUuid -> {
						if (folderUuid.isPresent()) {
							return Single.just(folderUuid.get());
						}

						NodeCreateRequest createRequest = new NodeCreateRequest()
								.setParentNodeUuid(formUuid)
								.setLanguage("en")
								.setSchemaName(SchemaInitializer.FOLDER_SCHEMA_NAME)
								.setPublish(publishFormFolder(folderName));

						createRequest.getFields().put("name", new StringFieldImpl().setString(folderName));

						return adminClient.createNode(projectName, createRequest).toSingle()
								.map(NodeResponse::getUuid);
					});
		});
	}

	private Completable ensureFormFolders(String projectName, String formUuid, String lang, String ...folderNames) throws IOException {
		Set<String> requiredFolders = new HashSet<>(Arrays.asList(folderNames));
		GraphQLRequest request = new GraphQLRequest()
			.setQuery(IOUtils.toString(FormsPlugin.class.getResource("findFormFolders.graphql"), StandardCharsets.UTF_8))
			.setVariables(new JsonObject().put("formUuid", formUuid).put("lang", lang));

		return userClient.graphql(projectName, request).toSingle()
			.map(rs -> Optional.ofNullable(rs.getData())
				.map(data -> data.getJsonObject("node"))
				.map(node -> node.getJsonObject("children"))
				.map(children -> children.getJsonArray("elements"))
				.orElse(new JsonArray())
				.stream()
				.filter(o -> o instanceof JsonObject)
				.map(o -> ((JsonObject) o).getString("displayName"))
				.collect(Collectors.toSet()))
			.flatMapObservable(existingFolders -> {
				requiredFolders.removeAll(existingFolders);

				return Observable.fromIterable(requiredFolders);
			})
			.flatMapCompletable(folderName -> {
				NodeCreateRequest createRequest = new NodeCreateRequest()
					.setParentNodeUuid(formUuid)
					.setLanguage("en")
					.setPublish(publishFormFolder(folderName))
					.setSchemaName(SchemaInitializer.FOLDER_SCHEMA_NAME);

				createRequest.getFields().put("name", new StringFieldImpl().setString(folderName));

				return userClient.createNode(projectName, createRequest).toCompletable();
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
		String name = getSchemaName(formUuid);
		// we do not let this single fail, if the schema cannot be loaded (e.g. due to lacking permission),
		// because this would return a 404, but we want a 403 (coming from the load call below)
		Single<Optional<SchemaResponse>> schema = schemaService.loadSchemaByName(name, false);
		return Single.zip(schema, load(projectName, formUuid), (formSchemaResponse, formNodeResponse) -> {
			if (formSchemaResponse.isEmpty()) {
				String message = String.format("Schema with name \"%s\" could not be found.", name);
				throw new FormsError(message, 404);
			}

			NumberField cmsIdField = formNodeResponse.getFields().getNumberField("cms_id");
			JsonObject formSchema = new JsonObject(formSchemaResponse.get().toJson());
			return formSchema.put("cms_id", cmsIdField != null ? cmsIdField.getNumber() : null);
		});
	}

	/**
	 * load form only in language en
	 * @param projectName
	 * @param uuid
	 * @return
	 */
	public Single<JsonObject> loadForm(String projectName, String uuid) {
		return load(projectName, uuid, "en").map(formNode -> {
			// TODO if the node has no fields, no specified language was found
			return new JsonObject(formNode.getFields().getStringField("json").getString());
		});
	}
	
	/**
	 * load form as admin in langugae de
	 */
	public Single<JsonObject> loadFormAsAdmin(String projectName, String uuid) {
		return loadFormAsAdmin(projectName, uuid, "en").map(formNode -> {
			return new JsonObject(formNode.getFields().getStringField("json").getString());
		});
	}


	/**
	 * Loads the form identified by {@code uuid} and assembles a Form Generator response.
	 *
	 * <p>When {@code formDataNode} is {@code null}, {@code data.properties} are populated from
	 * the schema {@code formGridOptions.defaultValue} entries. When a node is supplied, field
	 * values are read from that submission node instead.</p>
	 *
	 * @param projectName   Mesh project name
	 * @param uuid          UUID of the form node
	 * @param formID        client-supplied form instance identifier; must not be blank
	 * @param language      language code used for {@code possibleValues} label resolution
	 * @param formDataNode  optional previously stored submission node; {@code null} for default-value mode
	 * @param jwtClaims     decoded JWT payload claims from the request; used to resolve {@code userReference} defaults
	 * @return Single emitting the assembled {@link Form}
	 */
	public Single<Form> loadFormForFormGen(String projectName, String uuid, String formID, String language,
			NodeResponse formDataNode, Map<String, Object> jwtClaims) {
		if (StringUtils.isBlank(formID)) {
			return Single.error(new FormsError("Request body must contain non-empty 'formID'",
					HttpResponseStatus.BAD_REQUEST.code()));
		}
		if (formDataNode != null && isFormDataNodePublished(formDataNode)) {
			return Single.error(new FormsError(
					String.format("Form data with uuid \"%s\" is already published and cannot be loaded for editing.",
							formDataNode.getUuid()),
					HttpResponseStatus.CONFLICT.code()));
		}

		return statusService.getFormtypeConfigs(projectName, adminClient)
			.flatMap(formTypeConfigs -> loadForm(projectName, uuid)
				.flatMap(jsonObj -> Single.fromCallable(() -> {
					Form form = toForm(jsonObj.encode(), uuid, null);
					JsonNode formTypeConfig = formTypeConfigs.getConfigs().get(form.getType());
					if (formTypeConfig != null) {
						form.setProperty("flows", formTypeConfig.get("flows"));
					}
					form.setData(formDataNode != null
							? buildFormDataFromNode(MultiMap.caseInsensitiveMultiMap(), form, formID, Collections.emptyMap(), language, formDataNode)
							: buildDefaultFormData(MultiMap.caseInsensitiveMultiMap(), form, formID, Collections.emptyMap(), language, jwtClaims));
					String captchaId = form.getCaptchaId();
					if (StringUtils.isNotBlank(captchaId)) {
						int sep = captchaId.indexOf('_');
						if (sep > 0) {
							String provider = captchaId.substring(0, sep);
							String mode = captchaId.substring(sep + 1);
							String sitekey = resolveCaptchaSiteKey(provider);
							if(sitekey == null){
								throw new FormsError("Invalid captchaId:" + captchaId, HttpResponseStatus.BAD_REQUEST.code());
							}
							form.setCaptchaConfig(new CaptchaConfigModel(provider, mode, sitekey));

						}
					}
					return form;
				})));
	}

	private String resolveCaptchaSiteKey(String provider) {
		switch (provider) {
			case "recaptcha":       return configuration.getReCaptcha() != null ? configuration.getReCaptcha().getSiteKey() : null;
			case "friendlycaptcha": return configuration.getFriendlyCaptcha() != null ? configuration.getFriendlyCaptcha().getSiteKey() : null;
			default:                return null;
		}
	}

	/**
	 * Builds {@link FormData} using schema {@code formGridOptions.defaultValue} entries. {@code hasValidationErrors} is
	 * always {@code false} in this mode.
	 *
	 * @param postedData Posted form data
	 * @param form the loaded form definition
	 * @param formID client-supplied form instance identifier
	 * @param language language code for label and translation resolution
	 * @param jwtClaims decoded JWT payload claims; used to resolve {@code userReference} defaults
	 *
	 * @return populated {@link FormData}
	 */
	public static FormData buildDefaultFormData(MultiMap postedData, Form form, String formID, Map<String, ValidationError> errors, String language, Map<String, Object> jwtClaims) {
		return initFormData(postedData, form, formID, errors, (key, element) -> buildDefaultProperty(element, language, jwtClaims));
	}

	/**
	 * Builds {@link FormData} by reading stored field values from a Mesh submission node.
	 * {@code hasValidationErrors} reflects the node's {@code _errors} field.
	 *
	 * @param form     the loaded form definition
	 * @param formID   client-supplied form instance identifier
	 * @param language language code for {@code possibleValues} label resolution
	 * @param node     the Mesh submission node whose fields supply the property values
	 * @return populated {@link FormData} reflecting the stored submission
	 */
	public static FormData buildFormDataFromNode(MultiMap postedData, Form form, String formID, Map<String, ValidationError> errors, String language, NodeResponse node) {
		return initFormData(postedData, form, formID, errors, (key, element) -> buildPropertyFromNode(element, language, node));
	}

	/**
	 * Creates a {@link FormData} shell, iterates all schema properties, and populates the {@code properties} map by
	 * applying {@code propertyBuilder} to each element.
	 *
	 * @param postedData Posted form data
	 * @param form the loaded form definition
	 * @param formID client-supplied form instance identifier
	 * @param errors value for {@link FormData#setHasValidationErrors}
	 * @param propertyBuilder produces a {@link FormDataProperty} for each (key, element) pair
	 *
	 * @return fully assembled {@link FormData}
	 */
	private static FormData initFormData(MultiMap postedData, Form form, String formID, Map<String, ValidationError> errors,
	                                     BiFunction<String, FormElement, FormDataProperty> propertyBuilder) {
		FormData data = new FormData();
		data.setFormID(formID);
		data.setVersion(form.getSchema() != null ? form.getSchema().getVersion() : null);
		data.setHasValidationErrors(MapUtils.isNotEmpty(errors));
		Map<String, FormDataProperty> properties = new LinkedHashMap<>();
		if (form.getSchema() != null && form.getSchema().getProperties() != null) {
			form.getSchema().getProperties().forEach((key, element) -> {
				var prop = propertyBuilder.apply(key, element);

				if (postedData.contains(key)) {
					prop.setValue(postedData.get(key));
				}

				if (errors.containsKey(key)) {
					prop.setValidationMessage(errors.get(key).getMessage());
				}

				properties.put(key, prop);
			});
		}
		data.setProperties(properties);
		return data;
	}

	/**
	 * Builds a {@link FormDataProperty} for one schema element using {@code formGridOptions.defaultValue}.
	 *
	 * <p>When {@code selectOptions} are present the default value is mapped to a {@link PossibleValue}
	 * (list or scalar). Without options, the {@link DefaultValue} resolves itself via
	 * {@link DefaultValue#resolve(String, Map)} — covering SCALAR, LIST, USER_REFERENCE, and TRANSLATION.</p>
	 *
	 * @param element   schema element to build the property for
	 * @param language  language code for label and translation resolution
	 * @param jwtClaims decoded JWT payload claims; used to resolve {@code userReference} defaults
	 * @return populated {@link FormDataProperty}
	 */
	private static FormDataProperty buildDefaultProperty(FormElement element, String language, Map<String, Object> jwtClaims) {
		FormDataProperty prop = new FormDataProperty();
		DefaultValue defaultValue = element.getFormGridOptions().getDefaultValue();
		List<ValueListItem> selectOptions = resolveSelectOptions(element);

		if (selectOptions != null && !selectOptions.isEmpty()) {
			if (defaultValue != null && defaultValue.isList()) {
				// get the list of values (Ids) from selectoptions
				List<?> ids = (List<?>) defaultValue.resolve(language, jwtClaims);
				prop.setValues(ids.stream()
						.map(id -> (Object) findPossibleValue(String.valueOf(id), selectOptions, language, element))
						.filter(Objects::nonNull)
						.collect(Collectors.toList()));
			} else if (defaultValue != null) {
				prop.setValue(findPossibleValue(
						String.valueOf(defaultValue.resolve(language, jwtClaims)), selectOptions, language, element));
			}
			prop.setPossibleValues(buildPossibleValues(selectOptions, language, element));
		} else if (defaultValue != null) {
			if (defaultValue.isList()) {
				prop.setValues((List<Object>) defaultValue.resolve(language, jwtClaims));
			} else {
				prop.setValue(defaultValue.resolve(language, jwtClaims));
			}
		}
		return prop;
	}

	/**
	 * Builds a {@link FormDataProperty} for one schema element by reading its stored value from a
	 * Mesh submission node.
	 *
	 * <p>The field is looked up by {@link FormElement#getName()} on the node's field container.
	 * Elements with {@code selectOptions} map stored identifiers to {@link PossibleValue}s via the
	 * current {@code formGridOptions.selectOptions}; identifiers no longer present in the schema are
	 * silently dropped. If the node carries no field for this element,
	 * {@code value}/{@code values} is left unset.</p>
	 *
	 * @param element  schema element to build the property for
	 * @param language language code for {@code possibleValues} label resolution
	 * @param node     Mesh submission node supplying stored field values
	 * @return populated {@link FormDataProperty}
	 */
	private static FormDataProperty buildPropertyFromNode(FormElement element, String language, NodeResponse node) {
		FormDataProperty prop = new FormDataProperty();

		if (element instanceof BinaryElement) {
			BinaryField binaryField = node.getFields().getBinaryField(element.getName());
			if (binaryField != null) {
				Map<String, Object> binaryValue = new LinkedHashMap<>();
				binaryValue.put("fileName", binaryField.getFileName());
				binaryValue.put("size", binaryField.getFileSize());
				binaryValue.put("value", node.getUuid() + ":" + element.getName());
				prop.setValue(binaryValue);
			}
			return prop;
		}

		List<ValueListItem> selectOptions = resolveSelectOptions(element);

		if (selectOptions != null && !selectOptions.isEmpty()) {
			if (element.isList()) {
				StringFieldListImpl listField = node.getFields().getStringFieldList(element.getName());
				if (listField != null) {
					prop.setValues(listField.getItems().stream()
							.map(item -> (Object) findPossibleValue(String.valueOf(item), selectOptions, language, element))
							.filter(Objects::nonNull)
							.collect(Collectors.toList()));
				}
			} else {
				StringFieldImpl strField = node.getFields().getStringField(element.getName());
				if (strField != null) {
					prop.setValue(findPossibleValue(strField.getString(), selectOptions, language, element));
				}
			}
			prop.setPossibleValues(buildPossibleValues(selectOptions, language, element));
		} else {
			if (element.isList()) {
				StringFieldListImpl listField = node.getFields().getStringFieldList(element.getName());
				if (listField != null) {
					prop.setValues(new ArrayList<>(listField.getItems()));
				}
			} else {
				StringFieldImpl strField = node.getFields().getStringField(element.getName());
				if (strField != null) {
					prop.setValue(strField.getString());
				}
			}
		}
		return prop;
	}

	/**
	 * Converts a list of {@link ValueListItem}s to {@link PossibleValue}s.
	 * The display name for each item is resolved via {@link #resolveOptionLabel}.
	 *
	 * <p>For {@link AdminMailElement}, the {@code identifier} of each {@link PossibleValue} is set to
	 * the resolved display name instead of the raw option value, so that email addresses are never
	 * exposed to FormGen.</p>
	 *
	 * @param selectOptions source items; must not be {@code null}
	 * @param language      language code for display name resolution
	 * @param element       the form element owning the options; used to apply AdminMailElement masking
	 * @return list of {@link PossibleValue}s in the same order as {@code selectOptions}
	 */
	private static List<PossibleValue> buildPossibleValues(List<ValueListItem> selectOptions, String language, FormElement element) {
		return selectOptions.stream()
				.map(item -> {
					String displayName = resolveOptionLabel(item, language);
					String identifier = (element instanceof AdminMailElement)
							? displayName
							: StringUtils.defaultString(item.getValue());
					return new PossibleValue(identifier, displayName);
				})
				.collect(Collectors.toList());
	}

	/**
	 * Looks up {@code storedValue} in {@code options} and returns a {@link PossibleValue} with the
	 * display name resolved for {@code language}. Returns {@code null} when {@code options} is
	 * {@code null} or the stored value has no match.
	 *
	 * <p>For {@link AdminMailElement}, the returned {@link PossibleValue#getIdentifier()} is set to the
	 * resolved display name instead of the raw stored value, so that email addresses are never exposed
	 * to FormGen.</p>
	 *
	 * @param storedValue the raw option value stored in the Mesh node (e.g. an email address)
	 * @param options     available select options; may be {@code null}
	 * @param language    language code for display name resolution
	 * @param element     the form element owning the options; used to apply AdminMailElement masking
	 * @return matching {@link PossibleValue}, or {@code null}
	 */
	private static PossibleValue findPossibleValue(String identifier, List<ValueListItem> options, String language, FormElement element) {
		if (options == null) {
			return null;
		}
		return options.stream()
				.filter(item -> identifier.equals(item.getValue()))
				.findFirst()
				.map(item -> {
					String displayName = resolveOptionLabel(item, language);
					String storedIdentifier = (element instanceof AdminMailElement) ? displayName : identifier;
					return new PossibleValue(storedIdentifier, displayName);
				})
				.orElse(null);
	}

	/**
	 * Resolves the display label of {@code item} for {@code language}.
	 * Falls back to an empty string when the language key is absent.
	 *
	 * @param item     option item whose label map is queried
	 * @param language language code to look up
	 * @return resolved label, never {@code null}
	 */
	private static String resolveOptionLabel(ValueListItem item, String language) {
		Map<String, String> labels = item.getLabel();
		return (labels != null && language != null)
				? StringUtils.defaultString(labels.get(language))
				: "";
	}

	/**
	 * Returns the select options for {@code element}. Returns {@code null} when neither source
	 * provides a non-empty list.
	 *
	 * @param element form element to inspect
	 * @return non-empty list of options, or {@code null}
	 */
	private static List<ValueListItem> resolveSelectOptions(FormElement element) {
		List<ValueListItem> opts = element.getFormGridOptions().getSelectOptions();
		if (opts != null && !opts.isEmpty()) {
			return opts;
		}
		return null;
	}

	/**
	 * Remaps submitted values for {@link AdminMailElement} fields from their masked form (displayName used as
	 * identifier) back to the real stored identifiers (email addresses).
	 *
	 * <p>When FormGen receives a form, {@code AdminMailElement} options are sent with {@code identifier = displayName}
	 * to avoid exposing raw email addresses. On the way back in (save/submit/validate), this method reverses that
	 * mapping so the real email addresses are stored in Mesh.</p>
	 *
	 * @param form       the loaded form definition
	 * @param postedData raw posted data as received from FormGen
	 * @param language   language code used for display name resolution
	 * @return a new {@link MultiMap} with AdminMailElement values replaced by real identifiers; all other entries are unchanged
	 */
	public static MultiMap adminMailLabelToValue(Form form, MultiMap postedData, String language) {
		if (form == null || form.getSchema() == null || form.getSchema().getProperties() == null || postedData.isEmpty()) {
			return postedData;
		}
		MultiMap result = MultiMap.caseInsensitiveMultiMap();
		postedData.entries().forEach(e -> result.add(e.getKey(), e.getValue()));

		form.getSchema().getProperties().forEach((key, element) -> {
			if (!(element instanceof AdminMailElement)) {
				return;
			}
			List<String> submittedDisplayNames = postedData.getAll(key);
			if (submittedDisplayNames == null || submittedDisplayNames.isEmpty()) {
				return;
			}
			List<ValueListItem> options = resolveSelectOptions(element);
			if (options == null) {
				return;
			}
			result.remove(key);
			submittedDisplayNames.forEach(submittedDisplayName ->
				options.stream()
					.filter(item -> submittedDisplayName.equals(resolveOptionLabel(item, language)))
					.findFirst()
					.map(ValueListItem::getValue)
					.ifPresent(realId -> result.add(key, realId))
			);
		});
		return result;
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
		//TODO 8.5
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
		} catch (IOException | FormsError e) {
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
	public Form toForm(String formData, String formUuid, String language) throws IOException, FormsError {
		Form form = objectMapper.readValue(formData, Form.class);

		form.setUuid(formUuid);
		if (language != null) {
			form.setLanguage(language);
		}
		if (form.getSchema() == null || form.getSchema().getProperties() == null) {
			throw new FormsError("Form JSON must contain 'schema.properties'",
				HttpResponseStatus.BAD_REQUEST.code());
		}
		// Set name (map key) and configuration on each element once after deserialization
		form.getSchema().getProperties().forEach((key, element) -> {
			element.setName(key);
			element.setConfiguration(configuration);
		});

		return form;
	}

	/**
	 * Get the schema name for the form uuid
	 *
	 * @param formUuid
	 *            form uuid
	 * @return schema name
	 */
	public String getSchemaName(String formUuid) {
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
	 */
	private <T extends SchemaModel> Single<T> getSchema(NodeResponse formNode, T schema) {
		schema.setName(getSchemaName(formNode.getUuid()));
		schema.setContainer(false);
		try {
			Form form = toForm(formNode.getFields().getStringField("json").getString(), formNode.getUuid(),
				formNode.getLanguage());
			form.addFieldSchemas(schema);
		} catch (IOException | FormsError e) {
			return Single.error(new FormsError("Error while deserializing form JSON", e, HttpResponseStatus.BAD_REQUEST.code()));
		}

		// add field for errors
		schema.addField(new StringFieldSchemaImpl().setName(Form.ERRORS_FIELD_NAME).setLabel("Errors"));

		return Single.just(schema);
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

	private Single<SchemaResponse> createAndAssignSchema(String projectName, NodeResponse formNode) {
		return getSchema(formNode, new SchemaCreateRequest())
			.map(schema -> {
				log.info("Creating schema {}", schema.getName());
				return schema;
			})
			.flatMap(schema -> userClient.createSchema(schema).toSingle())
			.flatMap(createdSchema -> userClient.assignSchemaToProject(projectName, createdSchema.getUuid()).toSingle());
	}

	private Single<GenericMessageResponse> updateSchema(String projectName, NodeResponse formNode, SchemaResponse schema) {
		return getSchema(formNode, new SchemaUpdateRequest())
			.map(updateRequest -> {
				log.info("Updating schema {} with uuid {}", schema.getName(), schema.getUuid());
				return updateRequest;
			})
			.flatMap(updateRequest -> userClient.updateSchema(schema.getUuid(), updateRequest).toSingle())
			.flatMap(updateResponse -> userClient.assignSchemaToProject(projectName, schema.getUuid()).toCompletable().andThen(Single.just(updateResponse)));
	}

	/**
	 * Load the results for the specified poll form.
	 *
	 * @param pc The plugin context for the request
	 * @param formUuid The UUID of the form node
	 * @return A {@code Single} containing the results in a JSON array
	 */
	public Single<JsonObject> loadPollResults(PluginContext pc, String formUuid) {
		var projectName = pc.projectName();

		return loadAsAdmin(projectName, formUuid, true)
			.flatMap(this::toForm)
			.flatMap(form -> {
				if (!form.isSubmitOnce()) {
					return Single.error(new FormsError("Specified form is not a poll", HttpResponseStatus.BAD_REQUEST.code()));
				}

				var noResultsAvailable = CollectionUtils.isEmpty(pc.queryParam(PARAM_SHOW_RESULTS))
					&& Stream.of(pc.request().getCookie(FormDataService.COOKIE_NAME_VOTED))
						.filter(Objects::nonNull)
						.map(c -> c.getValue().split(FormDataService.COOKIE_VOTED_SEPARATOR))
						.flatMap(Arrays::stream)
						.noneMatch(form.getUuid()::equals);

				if (noResultsAvailable) {
					var msg = "Results require forcing results or form UUID in cookie '%s'".formatted(FormDataService.COOKIE_NAME_VOTED);

					return Single.error(new FormsError(msg, HttpResponseStatus.BAD_REQUEST.code()));
				}

				var pollResults = new JsonObject();

				return getFormFolderUuid(projectName, formUuid, new String[]{}, DATA_FOLDER_NAME)
					.flatMap(formDataFolderUuid -> loadFormData(projectName, formDataFolderUuid)
						.doOnNext(formData -> form.gatherPollResults(pollResults, formData))
						.ignoreElements()
						.andThen(Single.just(pollResults))
						.map(rs -> rs.stream()
							.filter(entry -> entry.getValue() instanceof JsonObject)
							.map(entry -> ((JsonObject) entry.getValue()).put("name", entry.getKey()))
							.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::addAll))))
					.map(results -> new JsonObject().put("results", results).put("templateContext", form.getTemplateContext()));
			});
	}

	/**
	 * Load the next batch of children for the given form UUID.
	 *
	 * @param project The project name
	 * @param parentUuid The UUID of the parent node (the form data folder)
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

	/**
	 * Determine whether the form folder with given name should be published
	 * @param name form folder name
	 * @return true for publish, false to not publish
	 */
	private boolean publishFormFolder(String name) {
		if (StringUtils.isEmpty(name)) {
			return false;
		}
		switch (name) {
		case DATA_FOLDER_NAME:
			return true;
		default:
			return false;
		}
	}

	private boolean isFormDataNodePublished(NodeResponse formDataNode) {
		Map<String, PublishStatusModel> languages = formDataNode.getAvailableLanguages();
		if (languages == null) return false;
		PublishStatusModel enStatus = languages.get("en");
		return enStatus != null && enStatus.isPublished();
	}
}
