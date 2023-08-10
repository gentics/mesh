package com.gentics.mesh.plugin.forms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.service.RenderService;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataSaveContext;
import com.gentics.mesh.plugin.forms.export.Exporter;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.FormType;
import com.gentics.mesh.plugin.forms.model.FormpageElement;
import com.gentics.mesh.plugin.forms.model.PostDataResponse;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class FormDataService {
	/**
	 * Name of the cookie, the user will get when a form is partially filled (the cookie value is the uuid of the form data node)
	 */
	public final static String COOKIE_NAME_DATA = "forms-data";

	public final static String COOKIE_NAME_VOTED = "forms-voted";

	public final static String COOKIE_VOTED_SEPARATOR = ":";

	public final static String PARAM_PUBLISHED_ONLY = "publishedOnly";

	private static final Logger log = LoggerFactory.getLogger(FormDataService.class);

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final Vertx vertx;

	private final FormsConfig configuration;

	private final ObjectMapper objectMapper;

	private final FormService formService;

	private final PagingService pagingService;

	private final MailService mailService;

	private final RenderService renderService;

	private final StatusService statusService;

	@Inject
	public FormDataService(
			@AdminClient MeshRestClient adminClient,
			@UserClient @Nullable MeshRestClient userClient,
			Vertx vertx,
			FormsConfig configuration,
			ObjectMapper objectMapper,
			FormService formService,
			PagingService pagingService,
			MailService mailService,
			RenderService renderService,
			StatusService statusService,
			PluginManifest manifest) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.vertx = vertx;
		this.configuration = configuration;
		this.objectMapper = objectMapper;
		this.formService = formService;
		this.pagingService = pagingService;
		this.mailService = mailService;
		this.renderService = renderService;
		this.statusService = statusService;
	}

	/**
	 * Save data posted by a user for the form
	 *
	 * @param ctx
	 *            Request context
	 * @return single of the post data response
	 */
	public Single<PostDataResponse> create(FormDataSaveContext ctx) {
		return formService.loadAsAdminWithLanguageFallback(ctx.getProjectName(), ctx.getFormUuid(), true)
			.doOnSuccess(ctx::setFormNode)
			.flatMap(formService::toForm)
			.doOnSuccess(ctx::setForm)
			.flatMap(form -> savePostedData(ctx));
	}

	/**
	 * Update already posted data
	 *
	 * @param ctx
	 *            Request context
	 * @return single of the post data response
	 */
	public Single<PostDataResponse> update(FormDataSaveContext ctx) {
		return Single.zip(
			formService.loadAsAdminWithLanguageFallback(ctx.getProjectName(), ctx.getFormUuid(), true),
			load(ctx.getProjectName(), ctx.getFormUuid(), ctx.getFormDataUuid()),
			(formNode, formDataNode) -> formService.toForm(formNode)
			.flatMap(form -> {
				ctx.setFormNode(formNode);
				ctx.setFormDataNode(formDataNode);
				ctx.setForm(form);
				return savePostedData(ctx);
			}))
			.flatMap(x -> x);
	}

	/**
	 * Load the form data by uuid. Only unpublished form data nodes will be returned, otherwise a 404 response is returned.
	 *
	 * @param projectName
	 *            project name
	 * @param formUuid
	 *            form uuid
	 * @param formDataUuid
	 *            form data uuid
	 * @return single noderesponse
	 */
	public Single<NodeResponse> load(String projectName, String formUuid, String formDataUuid) {
		return load(projectName, formUuid, formDataUuid, false);
	}
	/**
	 * Load the form data by uuid
	 *
	 * @param projectName
	 *            project name
	 * @param formUuid
	 *            form uuid
	 * @param formDataUuid
	 *            form data uuid
	 * @param allowPublished
	 *            Allow returning published form data. If not set to true, only unpublished form data nodes can be loaded, and for published nodes a 404 response is returned.
	 * @return single noderesponse
	 */
	public Single<NodeResponse> load(String projectName, String formUuid, String formDataUuid, boolean allowPublished) {
		return adminClient.findNodeByUuid(projectName, formDataUuid).toSingle()
			.flatMap(formDataNode -> {
				if (FormUtil.isFormDataNode(formDataNode) && formUuid.equals(formDataNode.getParentNode().getUuid()) && (allowPublished || !isFormDataPublished(formDataNode))) {
					return Single.just(formDataNode);
				} else {
					String message = String.format("Object with uuid \"%s\" could not be found.", formDataUuid);
					return Single.error(new FormsError(message, 404));
				}
			});
	}

	/**
	 * Export the data for the form with the specified UUID.
	 *
	 * The export format is determined via query parameters in the request, and the default format is CSV.
	 *
	 * @param projectName
	 *            project name
	 * @param formUuid
	 *            The UUID of the form to export
	 * @param context
	 *            The current routing context
	 * @param lang optional languages
	 * @return single export data
	 */
	public Single<String> export(String projectName, String formUuid, PluginContext context, String...lang) {
		// Export only published data nodes by default.
		boolean publishedOnly = Optional.ofNullable(context.queryParams().get(PARAM_PUBLISHED_ONLY))
			.map(Boolean::parseBoolean)
			.orElse(true);

		return statusService.languageFallback(projectName, lang)
			.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
			.flatMap(formService::toForm)
			.flatMap(form -> {
				Exporter exporter = Exporter.create(configuration, userClient, context, formUuid, SchemaService.getSchemaName(formUuid), configuration.getExportOptions(), getDownloadBaseUrl(form, projectName));

				return exporter.setFieldLabels(form.getReportableFields()).export(form, publishedOnly);
			});
	}

	/**
	 * Get the base URL for binary field downloads.
	 *
	 * <p>
	 *     The base URL must be a string such that
	 *     {@code getDownloadBaseUrl(form, project) + "/:formDataUuid/binary/:binaryFieldName"}
	 *     is a valid download URL.
	 * </p>
	 *
	 * <p>
	 *     When no {@code downloadBaseUrl} is specified in the form, the URL
	 *     will be the binary download endpoint for the forms plugin itself,
	 *     otherwise the provied {@code downloadBaseUrl} is used.
	 * </p>
	 *
	 * <p>
	 *     <strong>Important:</strong> Note that the form UUID must be part of
	 *     the string generated by {@code getDownloadBaseUrl()}.
	 * </p>
	 *
	 * @param form The current form.
	 * @param projectName The current project name.
	 * @return The base URL for binary field downloads.
	 */
	public static String getDownloadBaseUrl(Form form, String projectName) {
		String baseUrl = form.getDownloadBaseUrl();

		if (StringUtils.isBlank(baseUrl)) {
			return "/api/v2/" + projectName + "/plugins/forms/forms/" + form.getUuid() + "/data";
		}

		return StringUtils.removeEnd(baseUrl, "/");
	}

	/**
	 * Searches the data nodes of the specified form and sends the results to the meshClient.
	 *
	 * @param projectName project name
	 * @param formUuid the UUID of the form node
	 * @param query search query
	 * @param pagingParams paging parameters
	 * @param publishedOnly whether to include only published form data nodes
	 * @return single search response
	 */
	public Single<JsonObject> find(String projectName, String formUuid, String query, JsonObject pagingParams, boolean publishedOnly) {
		return statusService.languageFallback(projectName)
			.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
			.flatMap(formService::toForm)
			.flatMap(form -> {
				GraphQLRequest graphQlRequest = createGraphQlRequest(form, query, pagingParams);
				VersioningParameters params = new VersioningParametersImpl();

				if (publishedOnly) {
					params.published();
				} else {
					params.draft();
				}

				return adminClient.graphql(projectName, graphQlRequest, params).toSingle()
					.flatMap(graphQlResponse -> {
						JsonObject clientResponse = new JsonObject();
						JsonObject nodes = graphQlResponse.getData().getJsonObject("nodes");

						if (nodes != null) {
							pagingService.addPagingInfoToResponse(clientResponse, nodes);
							clientResponse.put("entries", nodes.getJsonArray("elements"));

							return Single.just(clientResponse.put("elements", new JsonObject(form.elementMap())));
						}

						return Single.error(new FormsError("Unexpected NULL search response.", 500));
					});
			});
	}

	/**
	 * Create a GraphQL request to load form data nodes for the specified form.
	 * Loads the specified form and creates a GraphQLRequest with a query to find the form data nodes (and the page) indicated by the HTTP query string.
	 *
	 * @param form The form to load form data for
	 * @param query The Elasticsearch query to use
	 * @param pagingParams Paging parameters for the GraphQL query
	 * @return a GraphQLRequest
	 * @throws IOException if loading the ElasticSearch or GraphQL query fails
	 */
	private GraphQLRequest createGraphQlRequest(Form form, String query, JsonObject pagingParams) throws IOException {
		String rawGraphQLQuery = IOUtils.toString(FormsPlugin.class.getResource("findFormData.graphql"), StandardCharsets.UTF_8);
		JsonObject graphQLParams = new JsonObject(pagingParams.toBuffer())
			.put("query", assembleFindFormDataESQuery(form.getUuid(), query).toString());
		Map<String, String> context = new HashMap<>();

		context.put("schemaName", SchemaService.getSchemaName(form.getUuid()));
		context.put("fieldNames", String.join("\n", form.getReportableElementNamesForGraphQL()));

		String graphQLQuery = new Handlebars().compileInline(rawGraphQLQuery).apply(Context.newContext(context));

		return new GraphQLRequest().setQuery(graphQLQuery).setVariables(graphQLParams);
	}

	/**
	 * Gets the binary field for the specified form data node and sends it to the meshClient.
	 *
	 * @param formUuid
	 *            the UUID of the form node
	 * @param formDataUuid
	 *            the UUID of the data node
	 * @param binaryFieldName
	 *            the name of the binary field in the form data node
	 */
	public Single<MeshResponse<MeshBinaryResponse>> loadBinary(String projectName, String formUuid, String formDataUuid, String binaryFieldName) {
		// Binaries can be loaded for already published form data as well, so call the variant of load() which allows loading published form data.
		return load(projectName, formUuid, formDataUuid, true).flatMap(formDataNode -> {
			return userClient.downloadBinaryField(projectName, formDataUuid, formDataNode.getLanguage(), binaryFieldName).getResponse();
		});
	}

	/**
	 * Deletes the form data with the specified formDataUuid.
	 *
	 * @param projectName
	 *            project name
	 * @param formUuid
	 *            the UUID of the form node to be deleted
	 * @param formDataUuid
	 *            the UUID of the data node
	 * @return single void
	 */
	public Single<EmptyResponse> delete(String projectName, String formUuid, String formDataUuid) {
		return load(projectName, formUuid, formDataUuid).flatMap(formDataNode -> {
			// loadFormDataByUuid() already checks if formDataUuid is a subnode of formUuid and if formUuid is actually a form.
			DeleteParametersImpl deleteParams = new DeleteParametersImpl();
			deleteParams.setRecursive(true);
			return userClient.deleteNode(projectName, formDataUuid, deleteParams).toSingle();
		});
	}

	/**
	 * Render the form with data
	 *
	 * @param projectName
	 *            project name
	 * @param formUuid
	 *            form UUID
	 * @param formDataInfo
	 *            form data UUID and optional form page element name (separated by comma)
	 * @param rc
	 *            routing context
	 * @param lang optional languages
	 * @return single buffer of rendered form
	 */
	public Single<Buffer> render(String projectName, String formUuid, String formDataInfo, RoutingContext rc, String...lang) {
		String formDataUuid;
		String currentPage;

		if (StringUtils.isNotEmpty(formDataInfo)) {
			String[] parts = formDataInfo.split("-");

			formDataUuid = parts[0];
			currentPage = parts.length > 1 ? parts[1] : null;
		} else {
			formDataUuid = formDataInfo;
			currentPage = null;
		}

		return statusService.getPathPrefixOfDefaultBranch(projectName)
			.flatMap(pathPrefix -> Single.zip(
				statusService.languageFallback(projectName, lang)
					.flatMap(langFallback -> formService.loadAsAdmin(projectName, formUuid, true, langFallback)),
				load(projectName, formUuid, formDataUuid).onErrorReturn(t -> null),
				(formNode, formDataNode) -> {
					Form form = formService.toForm(formNode.getFields().getStringField("json").getString(), formNode.getUuid())
						.removeInactiveElements();
					JsonObject formJson = form.prepareRenderContext(pathPrefix, configuration);

					return RxUtil.fromNullable(formDataNode)
						.flatMapSingleElement(fdn -> {
							// only fill in data, if form is not published
							if (!isFormDataPublished(fdn)) {
								JsonObject formDataJson = form.getFormDataJson(fdn);
								formJson.put("dataid", formDataUuid);
								mergeData(formJson, formDataJson);
							}

							Set<String> result;

							if (form.isCompleted(fdn)) {
								result = Collections.emptySet();
							} else {
								// get completed formpages
								result = form.flattened().filter(elem -> elem instanceof FormpageElement)
									.filter(elem -> elem.isCompleted(fdn))
									.map(FormElement::getName)
									.collect(Collectors.toSet());
							}

							return Single.just(result);
						})
						.toSingle(Collections.emptySet())
						.flatMap(completed -> {
							formService.markFormpages(formJson, currentPage, completed);

							rc.put("form", formJson);

							return renderService.rxRender(rc.data(), "form");
						});
				}))
				.flatMap(s -> s);
	}

	/**
	 * Validate and save the data to mesh. publish form, if completed. Redirect to success url or referer
	 *
	 * @param ctx
	 * @return single redirect URL
	 */
	private Single<PostDataResponse> savePostedData(FormDataSaveContext ctx) {
		Map<String, ValidationError> errors = ctx.getForm().validate(ctx.getPostedData(), ctx.getFileUploads(), ctx.getFormDataNode());
		return saveFormData(ctx.getProjectName(), ctx.getFormNode(), ctx.getFormDataNode(), ctx.getForm(), ctx.getPostedData(), errors)
			.flatMap(savedFormDataNode -> saveFileUploads(ctx.getProjectName(), savedFormDataNode, ctx.getFileUploads()))
			.flatMap(savedFileUploads -> {
				if (ctx.isPublish() && errors.isEmpty() && ctx.getForm().isCompleted(savedFileUploads)) {
					return publishFormData(ctx, savedFileUploads, errors);
				} else {
					// set the data cookie
					if (ctx.handleCookies()) {
						Cookie cookie = Cookie.cookie(COOKIE_NAME_DATA, ctx.getForm().getDataCookie(savedFileUploads.getUuid()));
						cookie.setPath(getPath(ctx.getReferer()));
						cookie.setHttpOnly(true);
						ctx.getContext().addCookie(cookie);
					}

					PostDataResponse response = new PostDataResponse();
					response.setDataUuid(savedFileUploads.getUuid());
					response.setCreated(savedFileUploads.getCreated());
					response.setErrors(errors);
					// TODO Set this to false?
					response.setFinished(true);
					if (ctx.getReferer() != null) {
						response.setRedirectUrl(ctx.getReferer());
					} else {
						response.setRedirectUrl("");
					}

					return Single.just(response);
				}
			});
	}

	/**
	 * Save the form data to mesh
	 *
	 * @param projectName
	 *            project name
	 * @param formNode
	 *            form node
	 * @param formDataNode
	 *            optional form data node (for updating)
	 * @param form
	 *            form object
	 * @param postedData
	 *            posted data
	 * @param errors
	 *            map containing validation errors
	 * @return single of the node response
	 */
	private Single<NodeResponse> saveFormData(String projectName, NodeResponse formNode, NodeResponse formDataNode, Form form, MultiMap postedData,
		Map<String, ValidationError> errors) {
		FieldContainer container = null;
		NodeCreateRequest createRequest = null;
		NodeUpdateRequest updateRequest = null;

		if (formDataNode == null) {
			createRequest = new NodeCreateRequest().setSchemaName(SchemaService.getSchemaName(formNode.getUuid()))
				.setParentNodeUuid(formNode.getUuid())
				.setLanguage("en");
			container = createRequest;
		} else {
			updateRequest = new NodeUpdateRequest();
			updateRequest.setVersion(formDataNode.getVersion());
			updateRequest.setLanguage("en");
			container = updateRequest;
		}

		form.addFields(container, postedData, !errors.isEmpty());
		form.updateFormPage(postedData);

		if (!errors.isEmpty()) {
			try {
				container.getFields().put(Form.ERRORS_FIELD_NAME, new StringFieldImpl().setString(objectMapper.writeValueAsString(errors)));
			} catch (JsonProcessingException e) {
				return Single.error(e);
			}
		} else {
			container.getFields().put(Form.ERRORS_FIELD_NAME, new StringFieldImpl().setString(""));
		}

		if (formDataNode == null) {
			return adminClient.createNode(projectName, createRequest).toSingle();
		} else {
			return adminClient.updateNode(projectName, formDataNode.getUuid(), updateRequest).toSingle();
		}
	}

	/**
	 * Save fileuploads to mesh
	 *
	 * @param projectName
	 *            project name
	 * @param formDataNode
	 *            form data node
	 * @param fileUploads
	 *            validated file uploads
	 * @return single of the form data node
	 */
	private Single<NodeResponse> saveFileUploads(String projectName, NodeResponse formDataNode, Set<FileUpload> fileUploads) {
		if (fileUploads.isEmpty()) {
			return Single.just(formDataNode);
		}

		return Flowable.fromIterable(fileUploads)
			// The upload can be blocking in some implementations of the rest client. Therefore we queue
			// it as a worker job just in case.
			.flatMapSingle(fileUpload -> vertx.<NodeResponse>rxExecuteBlocking(prm -> {
				long size = fileUpload.size();
				InputStream ins;
				try {
					ins = new FileInputStream(fileUpload.uploadedFileName());
				} catch (FileNotFoundException e) {
					prm.fail(e);
					return;
				}
				adminClient.updateNodeBinaryField(projectName, formDataNode.getUuid(), formDataNode.getLanguage(), formDataNode.getVersion(),
					fileUpload.name(),
					ins, size, fileUpload.fileName(), fileUpload.contentType())
				.toSingle().subscribe(prm::complete, prm::fail);
			}).toSingle(), false, 1)
		.lastOrError();
	}

	/**
	 * Publish the form data.
	 * This will also send mails to the configured users, remove the data cookie and create a response.
	 *
	 * @param ctx
	 *            Request context
	 * @param savedFileUploads
	 *            saved file uploads from {@link #saveFileUploads(String, NodeResponse, Set)}
	 * @param errors
	 * @return single
	 */
	private Single<PostDataResponse> publishFormData(FormDataSaveContext ctx, NodeResponse savedFileUploads, Map<String, ValidationError> errors) {
		return adminClient.publishNode(ctx.getProjectName(), savedFileUploads.getUuid()).toSingle().map(publishStatus -> {
			PluginContext meshContext = ctx.getContext();
			Form form = ctx.getForm();
			MultiMap postedData = ctx.getPostedData();

			// send mails to admin and user
			mailService.sendMails(ctx.getProjectName(), ctx.getForm(), savedFileUploads, meshContext);

			// perform actions
			ctx.getForm().flattened().forEach(e-> e.performSubmit(ctx.getForm(), postedData));

			boolean isPoll = ctx.getForm().getType() == FormType.POLL;

			// remove the data cookie
			if (ctx.handleCookies()) {
				meshContext.removeCookie(COOKIE_NAME_DATA);
				Cookie cookie = Cookie.cookie(COOKIE_NAME_DATA, "");
				cookie.setPath(getPath(ctx.getReferer()));
				cookie.setHttpOnly(true);
				cookie.setMaxAge(0);
				meshContext.addCookie(cookie);

				if (isPoll) {
					addPollCookie(ctx);
				}
			}

			PostDataResponse response = new PostDataResponse();
			response.setDataUuid(savedFileUploads.getUuid());
			response.setCreated(savedFileUploads.getCreated());
			response.setErrors(errors);
			response.setFinished(true);
			if (form.getSuccessurl() != null) {
				response.setRedirectUrl(form.getSuccessurl());
			} else if (isPoll) {
				response.setRedirectUrl(ctx.getReferer());
			} else {
				response.setRedirectUrl("");
			}

			return response;
		});
	}

	/**
	 * Add or update the {@link #COOKIE_NAME_VOTED forms-voted} cookie to
	 * include the UUID of the form for which data was saved.
	 *
	 * <p>
	 *     The form UUIDs in the resulting cookie are delimited by a colon
	 *     ({@code :}).
	 * </p>
	 *
	 * @param ctx The current form data save context
	 */
	private void addPollCookie(FormDataSaveContext ctx) {
		PluginContext pc = ctx.getContext();
		String formUuid = ctx.getFormUuid();
		String votedPolls = Optional.ofNullable(pc.getCookie(COOKIE_NAME_VOTED))
			.map(Cookie::getValue)
			.filter(StringUtils::isNotBlank)
			.map(old -> {
				String[] uuids = old.split(COOKIE_VOTED_SEPARATOR);
				// Initialise the set big enough to avoid resizing (0.75 is the default load factor).
				Set<String> voted = new HashSet<>((int) (uuids.length / .75f) + 1);

				// Make sure every UUID is present only once by putting them in a set, ...
				for (String uuid : uuids) {
					voted.add(uuid);
				}

				voted.add(formUuid);

				// ... and that there are no empty "fields" in the cookie value.
				voted.removeIf(StringUtils::isBlank);

				return String.join(COOKIE_VOTED_SEPARATOR, voted);
			})
			.orElse(formUuid);

		Cookie cookie = Cookie.cookie(COOKIE_NAME_VOTED, votedPolls)
			// The cookie is needed when rendering the form in a page, as well as when data is submitted to the
			// /api/forms/forms/ID/data endpoint, so we set the path to /.
			.setPath("/")
			.setHttpOnly(true)
			.setMaxAge(Integer.MAX_VALUE);

		pc.addCookie(cookie);
	}

	/**
	 * Get the path from the given URL
	 *
	 * @param url
	 *            URL
	 * @return path
	 */
	private String getPath(String url) {
		if (!StringUtils.isBlank(url)) {
			try {
				URL refererUrl = new URL(url);
				return refererUrl.getPath();
			} catch (MalformedURLException e) {
			}
		}

		return url;
	}

	/**
	 * Creates an ElasticSearch query to find data nodes for the specified form with the search string (if any) that is contained in the HTTP query string.
	 *
	 * @param formUuid
	 *            the UUID of the form
	 * @param userQuery
	 *            the value of the 'q' parameter from the HTTP query string (may be null)
	 * @return the ElasticSearch query as a JsonObject
	 * @throws IOException
	 *             if loading the ElasticSearch query fails
	 */
	private JsonObject assembleFindFormDataESQuery(String formUuid, String userQuery) throws IOException {
		String esQueryStr = IOUtils.toString(FormsPlugin.class.getResource("findFormDataES.json"), "UTF-8");
		JsonObject esQuery = new JsonObject(esQueryStr);
		String formSchema = SchemaService.getSchemaName(formUuid);

		JsonArray mustQuery = esQuery.getJsonObject("query").getJsonObject("bool").getJsonArray("must");
		mustQuery.getJsonObject(0).getJsonObject("term").put("schema.name.raw", formSchema);
		if (userQuery != null && !userQuery.isEmpty()) {
			mustQuery.getJsonObject(1).getJsonObject("multi_match").put("query", userQuery);
		} else {
			mustQuery.remove(1);
		}
		return esQuery;
	}

	/**
	 * Merge the form data into the form
	 *
	 * @param form
	 *            form
	 * @param formData
	 *            form data
	 */
	private void mergeData(JsonObject form, JsonObject formData) {
		for (String elementName : formData.fieldNames()) {
			JsonObject element = formService.findElement(form, elementName);
			if (element != null) {
				element.put("data", formData.getJsonObject(elementName));
			}
		}
	}

	/**
	 * Check whether the form data node is published
	 * @param formDataNode form data node
	 * @return true when it is published
	 */
	private boolean isFormDataPublished(NodeResponse formDataNode) {
		Map<String, PublishStatusModel> languages = formDataNode.getAvailableLanguages();
		for (PublishStatusModel status : languages.values()) {
			if (status.isPublished()) {
				return true;
			}
		}
		return false;
	}
}
