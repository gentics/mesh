package com.gentics.mesh.plugin.forms.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.service.RenderService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.export.ExportData;
import com.gentics.mesh.plugin.forms.export.Exporter;
import com.gentics.mesh.plugin.forms.model.FileElement;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.FormType;
import com.gentics.mesh.plugin.forms.model.FormpageElement;
import com.gentics.mesh.plugin.forms.model.PostDataResponse;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.plugin.forms.util.FormParameterUtil;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UUIDUtil;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

@Singleton
public class FormDataService {
	/**
	 * Name of the env variable for configuring the maximum allowed parallel move calls for the data migration
	 */
	public final static String MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_MOVE_CALLS = "MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_MOVE_CALLS";

	/**
	 * Name of the env variable for configuring the number of nodes fetched per page for the data migration
	 */
	public final static String MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_FETCH_NODES = "MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_FETCH_NODES";

	/**
	 * Name of the cookie, the user will get when a form is partially filled (the cookie value is the uuid of the form data node)
	 */
	public final static String COOKIE_NAME_DATA = "forms-data";

	public final static String COOKIE_NAME_VOTED = "forms-voted";

	public final static String COOKIE_VOTED_SEPARATOR = ":";

	public final static String PARAM_PUBLISHED_ONLY = "publishedOnly";

	public final static String POLL_VALUES_NAME = "pollValues";

	public final static int TEST_DELAY_MS = 5_000;

	private static final Logger log = LoggerFactory.getLogger(FormDataService.class);
	private static final String FIELD_DOWNLOAD_READY = "ready";
	private static final String FIELD_DOWNLOAD_BINARY = "download";
	private static final String FIELD_DOWNLOAD_ERROR = "error";
	private static final String MESSAGE_DOWNLOAD_CHECK_PREFIX = "plugins.forms.download.";

	/** DateTime formatter creating dates of the form "YYYY-MM-DD_HHmmss". */
	private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
		.parseCaseInsensitive()
		.append(DateTimeFormatter.ISO_LOCAL_DATE)
		.appendLiteral('_')
		.appendValue(ChronoField.HOUR_OF_DAY, 2)
		.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
		.appendValue(ChronoField.SECOND_OF_MINUTE, 2)
		.toFormatter();

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final Vertx rxVertx;

	private final FormsConfig configuration;

	private final ObjectMapper objectMapper;

	private final FormService formService;

	private final PagingService pagingService;

	private final MailService mailService;

	private final RenderService renderService;

	private final StatusService statusService;

	private final OkHttpClientService okHttpClientService;

	private final File storageDir;

	private final String loadFormDataToMigrateGraphQl;

	@Inject
	public FormDataService(
			@AdminClient MeshRestClient adminClient,
			@UserClient @Nullable MeshRestClient userClient,
			io.vertx.core.Vertx vertx,
			FormsConfig configuration,
			ObjectMapper objectMapper,
			FormService formService,
			PagingService pagingService,
			MailService mailService,
			RenderService renderService,
			StatusService statusService,
			OkHttpClientService okHttpClientService,
			PluginManifest manifest,
			@StorageDir File storageDir) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.rxVertx = new Vertx(vertx);
		this.configuration = configuration;
		this.objectMapper = objectMapper;
		this.formService = formService;
		this.pagingService = pagingService;
		this.mailService = mailService;
		this.renderService = renderService;
		this.statusService = statusService;
		this.okHttpClientService = okHttpClientService;
		this.storageDir = storageDir;

		try {
			this.loadFormDataToMigrateGraphQl = IOUtils.toString(FormsPlugin.class.getResource("loadFormDataToMigrate.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Initialize the service. This will migrate all form data nodes, which are created directly under the plugin root node
	 * @return completable
	 */
	public Completable initialize() {
		return statusService.projectsWithPluginActive().flatMapCompletable(this::migrateFormDataNodes);
	}

	/**
	 * Migrate form data nodes for all forms of the project
	 * @param project project
	 * @return completable
	 */
	private Completable migrateFormDataNodes(ProjectResponse project) {
		String projectName = project.getName();

		return statusService.languageFallback(projectName).flatMapCompletable(lang -> {
			return formService.listUuids(project.getName())
					.flatMapCompletable(formUuid -> migrateFormDataNodes(project, formUuid));
		});
	}

	/**
	 * Migrate form data nodes for the given form
	 * @param project project
	 * @param formUuid form uuid
	 * @param lang languages
	 * @return completable
	 */
	private Completable migrateFormDataNodes(ProjectResponse project, String formUuid, String...lang) {
		int maxConcurrentMoveCalls = getOptionalConfig(MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_MOVE_CALLS, 10, Integer::parseInt, Object::toString);
		int nodesPerPage = getOptionalConfig(MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_FETCH_NODES, 10_000, Integer::parseInt, Object::toString);

		String schemaName = formService.getSchemaName(formUuid);
		String projectName = project.getName();

		GraphQLRequest request = new GraphQLRequest()
				.setQuery(loadFormDataToMigrateGraphQl)
				.setVariables(new JsonObject().put("uuid", formUuid).put("schema", schemaName).put("perPage", nodesPerPage));
		VersioningParameters params = new VersioningParametersImpl().draft();

		return getFormDataFolderUuid(projectName, formUuid, lang)
			   .flatMapCompletable(formDataFolderUuid -> {
					   AtomicBoolean hasMore = new AtomicBoolean(false);
					   return adminClient.graphql(projectName, request, params).toSingle()
							   .flatMapObservable(response -> {
									   List<String> uuids = FormUtil.getFromResponse(response, "$.data.node.children.elements[*].uuid");
									   log.info("{} form data nodes found for migration of project {} / form {}", uuids.size(), projectName, formUuid);
									   hasMore.set(uuids.size() >= nodesPerPage);
									   return Observable.fromIterable(uuids);
							   })
							   .flatMap(dataUuid -> adminClient.moveNode(projectName, dataUuid, formDataFolderUuid).toObservable()
											   .doOnSubscribe(disp -> log.debug(String.format("Move misplaced form data node %s for form %s in project %s", dataUuid, formUuid, projectName))), 
									   maxConcurrentMoveCalls)
							   .flatMapCompletable(disp -> Completable.complete())
							   .repeatWhen(handler -> handler.takeWhile((o) -> hasMore.get()));
			   }).doOnSubscribe(disp -> log.debug(String.format("Checking misplaced form data nodes for form %s in project %s", formUuid, projectName)));

	}

	/**
	 * Save data posted by a user for the form
	 *
	 * @param ctx Request context
	 * @return single of the post data response
	 */
	public Single<PostDataResponse> create(FormDataCreateContext ctx) {
		return formService.loadAsAdminWithLanguageFallback(ctx.getProjectName(), ctx.getFormUuid(), true, ctx.getLanguages())
			.doOnSuccess(ctx::setFormNode)
			.flatMap(formService::toForm)
			.doOnSuccess(ctx::setForm)
			.flatMap(form -> savePostedData(ctx));
	}

	/**
	 * Update already posted data
	 *
	 * @param ctx Request context
	 * @return single of the post data response
	 */
	public Single<PostDataResponse> update(FormDataUpdateContext ctx) {
		return Single.zip(
			formService.loadAsAdminWithLanguageFallback(ctx.getProjectName(), ctx.getFormUuid(), true, ctx.getLanguages()),
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
	 *			project name
	 * @param formUuid
	 *			form uuid
	 * @param formDataUuid
	 *			form data uuid
	 * @return single noderesponse
	 */
	public Single<NodeResponse> load(String projectName, String formUuid, String formDataUuid) {
		return load(projectName, formUuid, formDataUuid, false);
	}
	/**
	 * Load the form data by uuid
	 *
	 * @param projectName project name
	 * @param formUuid form uuid
	 * @param formDataUuid form data uuid
	 * @param allowPublished
	 *			Allow returning published form data. If not set to true, only unpublished form data nodes can be loaded, and for published nodes a 404 response is returned.
	 * @return single noderesponse
	 */
	public Single<NodeResponse> load(String projectName, String formUuid, String formDataUuid, boolean allowPublished) {
		Single<NodeResponse> singleFormDataNode = adminClient.findNodeByUuid(projectName, formDataUuid).toSingle();
		Single<String> singleFormDataFolderUuid = getFormDataFolderUuid(projectName, formUuid);

		return singleFormDataNode.flatMap(formDataNode -> {
			if (FormUtil.isFormDataNode(formDataNode)) {
				return singleFormDataFolderUuid.flatMap(formDataFolderUuid -> {
					if (formDataFolderUuid.equals(formDataNode.getParentNode().getUuid()) && (allowPublished || !isFormDataPublished(formDataNode))) {
						return Single.just(formDataNode);
					} else {
						String message = String.format("Object with uuid \"%s\" could not be found.", formDataUuid);
						return Single.error(new FormsError(message, 404));
					}
				});
			} else {
				String message = String.format("Object with uuid \"%s\" could not be found.", formDataUuid);
				return Single.error(new FormsError(message, 404));
			}
		});
	}

	/**
	 * Create an export node for the given form.
	 *
	 * <p>
	 * The created export nodes UUID will be returned immediately in the response, the export itself will be started in
	 * the background.
	 * </p>
	 *
	 * @param projectName The project name.
	 * @param formUuid The UUID of the form to export data for.
	 * @param context The current request context.
	 * @param lang The requested languages.
	 * @return The created export node.
	 */
	public Single<NodeResponse> requestExport(String projectName, String formUuid, PluginContext context, String... lang)  {
		String options = FormParameterUtil.getExportOptions(context, configuration);

		return formService.getFormFolderUuid(projectName, formUuid, lang, FormService.DOWNLOADS_FOLDER_NAME)
			.flatMap(folderUuid -> formService.loadForm(projectName, formUuid, lang)
				.flatMap(form -> createDownloadNode(projectName, folderUuid, "export", "form-export", form.getString("name"), options)))
			.flatMap(exportNode -> {
				MessageConsumer<Object> consumer = rxVertx.eventBus()
					.consumer(MESSAGE_DOWNLOAD_CHECK_PREFIX + exportNode.getUuid(), msg -> msg.reply(true));

				Single<ExportData> export = export(projectName, formUuid, context, lang);

				if (configuration.isTesting()) {
					export = export.delay(TEST_DELAY_MS, TimeUnit.MILLISECONDS);
				}

				export.subscribe(exportData -> saveDownloadData(projectName, exportNode, exportData.getExportFile(),
						exportData.getFormat().fileExtension, exportData.getFormat().mimeType)
						.doFinally(consumer::rxUnregister).subscribe(), error -> {
							setDownloadError(projectName, exportNode, error.getLocalizedMessage())
									.doFinally(consumer::rxUnregister).subscribe();
						});

				return Single.just(exportNode);
			});
	}

	/**
	 * Request a download for all submitted binaries of the fiven form.
	 *
	 * @param projectName The project name.
	 * @param formUuid The form to download binaries for.
	 * @param languages The requested languages.
	 * @return The created download node for the binary download.
	 */
	public Single<NodeResponse> requestZipBinaries(String projectName, String formUuid, String ...languages) {
		return formService.getFormFolderUuid(projectName, formUuid, languages, FormService.DOWNLOADS_FOLDER_NAME)
			.flatMap(folderUuid -> statusService.languageFallback(projectName, languages)
				.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
				.flatMap(formService::toForm)
				.flatMap(form -> createDownloadNode(projectName, folderUuid, "binaries", "binaries", form.getName(), "")
					.flatMap(downloadNode -> {
						MessageConsumer<Object> consumer = rxVertx.eventBus()
							.consumer(MESSAGE_DOWNLOAD_CHECK_PREFIX + downloadNode.getUuid(), msg -> msg.reply(true));

						Single<Pair<Form, File>> binaries = zipBinaries(projectName, form);

						if (configuration.isTesting()) {
							binaries = binaries.delay(TEST_DELAY_MS, TimeUnit.MILLISECONDS);
						}

						binaries.map(Pair::getRight)
							.subscribe(downloadFile -> {
								saveDownloadData(projectName, downloadNode, downloadFile, "zip", "application/zip")
									.andThen(consumer.rxUnregister())
									.subscribe();
							}, error -> {
								setDownloadError(projectName, downloadNode, error.getLocalizedMessage())
									.andThen(consumer.rxUnregister()).subscribe();
							});

						return Single.just(downloadNode);
					})));
	}

	/**
	 * Create a download node in the node with the given root UUID.
	 *
	 * <p>
	 *	 The filename of the binary part will be constructed from {@code downloadPrefix_downloadName_currentDateTime}.
	 * </p>
	 *
	 * @param projectName The project name.
	 * @param rootUuid The UUID of the download root node.
	 * @param type The download type (currently export or binaries).
	 * @param downloadPrefix The prefix for the binary field filename.
	 * @param downloadName The name of the download for the filename.
	 * @param options Download options to store with the download (e.g. export type).
	 * @return The {@code NodeResponse} for the created download node.
	 */
	private Single<NodeResponse> createDownloadNode(String projectName, String rootUuid, String type, String downloadPrefix, String downloadName, String options) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setSchemaName(SchemaInitializer.DOWNLOAD_SCHEMA_NAME)
			.setParentNodeUuid(rootUuid)
			.setLanguage("en");
		FieldMap fields = request.getFields();
		String currentDateTime = ZonedDateTime.now(ZoneOffset.UTC).format(DATETIME_FORMATTER);

		fields.put(FIELD_DOWNLOAD_READY, new BooleanFieldImpl().setValue(false));
		fields.put("name", new StringFieldImpl().setString(String.format("%s_%s_%s", downloadPrefix, downloadName, currentDateTime)));
		fields.put("type", new StringFieldImpl().setString(type));
		fields.put("options", new StringFieldImpl().setString(options));

		return adminClient.createNode(projectName, request).toSingle();
	}
	/**
	 * Save the created download data in the given download node.
	 *
	 * @param projectName The project name.
	 * @param downloadNode The download node to save the data in.
	 * @param file The file to save in the binary field.
	 * @param fileExtension The file extension.
	 * @param mimeType The files MIME type.
	 */
	private Completable saveDownloadData(String projectName, NodeResponse downloadNode, File file, String fileExtension, String mimeType) {
		String name = downloadNode.getFields().getStringField("name").getString();
		FileInputStream fis;

		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			return Completable.error(e);
		}

		return adminClient.updateNodeBinaryField(
				projectName,
				downloadNode.getUuid(),
				downloadNode.getLanguage(),
				downloadNode.getVersion(),
				FIELD_DOWNLOAD_BINARY,
				fis,
				file.length(),
				String.format("%s.%s", name, fileExtension),
				mimeType)
			.toSingle()
			.flatMapCompletable(nodeResponse -> {
				NodeUpdateRequest statusUpdate = new NodeUpdateRequest().setLanguage("en");

				statusUpdate.getFields().put(FIELD_DOWNLOAD_READY, new BooleanFieldImpl().setValue(true));

				return adminClient.updateNode(projectName, nodeResponse.getUuid(), statusUpdate).toCompletable();
			})
			.doFinally(() -> {
				fis.close();
				file.delete();
			});
	}

	/**
	 * Save the download error in the download node
	 * @param projectName project name
	 * @param downloadNode download node
	 * @param error error message to save
	 * @return completable
	 */
	private Completable setDownloadError(String projectName, NodeResponse downloadNode, String error) {
		NodeUpdateRequest update = new NodeUpdateRequest().setLanguage("en");
		update.getFields().put(FIELD_DOWNLOAD_ERROR, new StringFieldImpl().setString(error));
		return adminClient.updateNode(projectName, downloadNode.getUuid(), update).toCompletable();
	}

	/**
	 * Check if the specified download is ready.
	 *
	 * <p>
	 * If the download has been created and saved in the node, the response will
	 * contain the field {@code "downloadReady": true} as well as the UUID for
	 * the export data itself (field {@code "downloadUuid"}) and its creation
	 * timestamp (field {@code "downloadTimestamp"}.
	 * </p>
	 *
	 * <p>
	 *	 If a download is currently prepared, the field {@code "downloadPending"}
	 *	 will be {@code true}.
	 * </p>
	 *
	 * <p>
	 * Otherwise {@code "downloadReady"} and {@code "downloadPending"} will be
	 * {@code false} and if an error occurred, the error message will be added
	 * to the field {@code "error"}.
	 * </p>
	 *
	 * @see #createDownloadInfo(String,GraphQLResponse)
	 *
	 * @param projectName The project name.
	 * @param formUuid The forms UUID.
	 * @param options Download options to store with the download (e.g. export type).
	 * @param lang The requested languages.
	 * @param downloadType The UUID of the download node.
	 * @return Status of the download node, and the download URL if the download is ready.
	 */
	public Single<JsonObject> checkDownload(String projectName, String formUuid, String options, String[] lang, String downloadType) {
		String query;

		try {
			query = IOUtils.toString(FormsPlugin.class.getResource("findFormDownloads.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return Single.error(e);
		}

		return formService.getFormFolderUuid(projectName, formUuid, lang, FormService.DOWNLOADS_FOLDER_NAME)
			.map(downloadFolderUuid -> new JsonObject().put("folderUuid", downloadFolderUuid).put("type", downloadType).put("options", options))
			.map(variables -> new GraphQLRequest().setQuery(query).setVariables(variables))
			.flatMap(request -> userClient.graphql(projectName, request).toSingle())
			.flatMap(result -> createDownloadInfo(projectName, result))
			.onErrorReturn(cause -> new JsonObject().put("downloadReady", false).put("downloadPending", false).put("error", cause.getMessage()));
	}

	/**
	 * Create the download info from the given GraphQL response.
	 *
	 * <p>
	 *	 The info will contain if a download is currently being created
	 *	 (field {@code requestPending}), whether there is a download that is
	 *	 ready (field {@code downloadReady}), the UUID and creation timestamp
	 *	 of the most recent ready download (fields {@code downloadUuid} and
	 *	 {@code downloadTimestamp}).
	 * </p>
	 *
	 * <p>
	 *	 Before the info is returned each found pending request is checked if
	 *	 there is still some instance working on it (by requesting a message
	 *	 over the event bus). If not the request node is deleted.
	 * </p>
	 *
	 * @param projectName The project name.
	 * @param graphQLResponse The GraphQL response with download information.
	 * @return A JSON object containing information about donwloads.
	 */
	private Single<JsonObject> createDownloadInfo(String projectName, GraphQLResponse graphQLResponse) {
		Optional<JsonArray> result = Optional.ofNullable(graphQLResponse.getData())
			.map(data -> data.getJsonObject("node"))
			.map(node -> node.getJsonObject("children"))
			.map(children -> children.getJsonArray("elements"));

		if (!result.isPresent() || result.get().isEmpty()) {
			return Single.just(new JsonObject()
				.put("downloadReady", false)
				.put("requestPending", false));
		}

		JsonArray elements = result.get();
		Set<String> pendingRequests = new HashSet<>();
		String mostRecentDate = null;
		String mostRecentDownloadUuid = null;

		String mostRecentErrorDate = null;
		String mostRecentError = null;

		for (int ii = 0; ii < elements.size(); ii++) {
			JsonObject download = elements.getJsonObject(ii);
			boolean ready = download.getJsonObject("fields").getBoolean("ready");
			String error = download.getJsonObject("fields").getString("error");
			String curDate = download.getString("created");

			if (ready) {

				if (mostRecentDate == null || mostRecentDate.compareTo(curDate) < 0) {
					mostRecentDate = curDate;
					mostRecentDownloadUuid = download.getString("uuid");
				}
			} else if (StringUtils.isEmpty(error)) {
				pendingRequests.add(download.getString("uuid"));
			} else {
				if (mostRecentErrorDate == null || mostRecentErrorDate.compareTo(curDate) < 0) {
					mostRecentErrorDate = curDate;
					mostRecentError = error;
				}
			}
		}

		JsonObject downloadInfo = new JsonObject()
			.put("requestPending", !pendingRequests.isEmpty());

		if (mostRecentDate == null) {
			downloadInfo.put("downloadReady", false);
		} else {
			downloadInfo
				.put("downloadReady", true)
				.put("downloadUuid", mostRecentDownloadUuid)
				.put("downloadTimestamp", mostRecentDate);
		}

		if (!StringUtils.isEmpty(mostRecentError)) {
			downloadInfo.put("error", mostRecentError);
		}

		List<Completable> pendingRequestChecks = new ArrayList<>();

		/*
		 * A plugin instance working on a download (either export or binaries) must handle event bus messages with
		 * the address "plugins.forms.download.DOWNLOAD_NODE_UUID" so that stale download nodes can be deleted. For each
		 * pending request a message is sent to the respective address and if no response comes within five seconds,
		 * the stale download node is deleted.
		 *
		 * Stale download nodes happen when a Mesh instance working on an export or binaries zip file is shut down
		 * before the download is finished.
		 */
		for (String requestNodeUuid : pendingRequests) {
			Completable check = rxVertx.eventBus().<Boolean>rxRequest(MESSAGE_DOWNLOAD_CHECK_PREFIX + requestNodeUuid, 0)
				.doOnSubscribe(ignored -> log.debug("Checking download node {}", requestNodeUuid))
				.map(Message::body)
				.timeout(5, TimeUnit.SECONDS)
				.onErrorReturnItem(Boolean.FALSE)
				.flatMapCompletable(inProgress -> {
					if (inProgress) {
						return Completable.complete();
					}

					return userClient.deleteNode(projectName, requestNodeUuid).toCompletable()
						.doOnSubscribe(ignored -> log.info("Deleting stale download node: {}", requestNodeUuid))
						.doOnComplete(() -> pendingRequestChecks.remove(requestNodeUuid));
				});

			pendingRequestChecks.add(check);
		}

		return Completable.merge(pendingRequestChecks)
			.andThen(Single.defer(() -> Single.just(downloadInfo.put("requestPending", !pendingRequests.isEmpty()))));
	}

	/**
	 * Export the data for the form with the specified UUID.
	 * <p>
	 * The export format is determined via query parameters in the request, and the default format is CSV.
	 *
	 * @param projectName project name
	 * @param formUuid The UUID of the form to export
	 * @param context The current routing context
	 * @param lang optional languages
	 * @return single export data
	 */
	public Single<ExportData> export(String projectName, String formUuid, PluginContext context, String... lang) {
		// Export only published data nodes by default.
		boolean publishedOnly = Optional.ofNullable(context.queryParams().get(PARAM_PUBLISHED_ONLY))
			.map(Boolean::parseBoolean)
			.orElse(true);

		return statusService.languageFallback(projectName, lang)
			.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
			.flatMap(formService::toForm)
			.flatMap(form -> {
				Exporter exporter = Exporter.create(configuration, userClient, context, new File(System.getProperty("java.io.tmpdir")), formUuid, SchemaService.getSchemaName(formUuid), configuration.getExportOptions(), getDownloadBaseUrl(form, projectName));

				return exporter
					.setFieldLabels(form.getReportableFields())
					.export(form, publishedOnly);
			});
	}

	/**
	 * Load the form with the given UUID with language fallback and then create
	 * a zip file containing its uploaded binaries.
	 *
	 * @see #zipBinaries(String, Form)
	 * @param projectName The project name.
	 * @param uuid The UUID of the form to zip the binaries for.
	 * @param languages The requested languages.
	 * @return A {@code Pair} containing the form and the created zip file.
	 */
	public Single<Pair<Form, File>> zipBinaries(String projectName, String uuid, String ...languages) {
		return statusService.languageFallback(projectName, languages)
			.flatMap(langFallback -> formService.load(projectName, uuid, langFallback))
			.flatMap(formService::toForm)
			.flatMap(form -> zipBinaries(projectName, form));
	}

	/**
	 * Create a zip file containing the uploaded binaries for the given form.
	 * @param projectName The project name.
	 * @param form The forms to zip binaries for.
	 * @return A {@code Pair} containing the form and the created zip file.
	 */
	public Single<Pair<Form, File>> zipBinaries(String projectName, Form form) {
		Set<String> fileElementNames = form.flattened().filter(elem -> elem instanceof FileElement).map(FormElement::getName)
			.filter(Objects::nonNull).collect(Collectors.toSet());
		Set<String> fileFieldNames = form.flattened().filter(elem -> elem instanceof FileElement).map(FormElement::getGraphQLName)
			.filter(Objects::nonNull).collect(Collectors.toSet());

		Single<List<Downloadable>> singleDownloadables = null;
		if (fileElementNames.isEmpty()) {
			singleDownloadables = Single.just(Collections.emptyList());
		} else {
			singleDownloadables = getBinaryGraphQl(projectName, form.getUuid(), fileFieldNames)
				.flatMap(graphQlResponse -> extractDownloadables(projectName, fileElementNames, graphQlResponse));
		}

		return singleDownloadables
			.flatMap(downloadables -> zipBinaries(projectName, downloadables)
				.map(zipFile -> Pair.of(form, zipFile)));
	}

	/**
	 * Run the graphql query to find all data nodes with binaries
	 *
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param fileFieldNames set of file field names
	 * @return single graphql response
	 */
	protected Single<GraphQLResponse> getBinaryGraphQl(String projectName, String uuid, Set<String> fileFieldNames) {
		Map<String, String> renderCtx = new HashMap<>();
		renderCtx.put("schemaName", SchemaService.getSchemaName(uuid));
		renderCtx.put("fieldNames", String.join("\n", fileFieldNames));

		String query;

		try {
			String rawQuery = IOUtils.toString(
				FormsPlugin.class.getResource("loadFormData.graphql"),
				"UTF-8");
			query = new Handlebars().compileInline(rawQuery).apply(Context.newContext(renderCtx));

			GraphQLRequest request = new GraphQLRequest()
				.setQuery(query)
				.setVariables(new JsonObject().put("uuid", uuid).put("perPage", Integer.MAX_VALUE).put("page", 1));

			return adminClient.graphql(projectName, request).toSingle();

		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Extract the downloadables from the graphql response
	 *
	 * @param projectName project name
	 * @param fileElementNames set of file element names
	 * @param response graphql response
	 * @return single list of downloadables
	 */
	protected Single<List<Downloadable>> extractDownloadables(String projectName, Set<String> fileElementNames, GraphQLResponse response) {
		List<Downloadable> downloadables = new ArrayList<>();
		JsonObject data = response.getData();
		if (data == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonObject node = data.getJsonObject("node");
		if (node == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonObject dataNode = node.getJsonObject("data");
		if (dataNode == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonObject children = dataNode.getJsonObject("children");
		if (children == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonArray elements = children.getJsonArray("elements");
		if (elements == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.getJsonObject(i);
			String uuid = element.getString("uuid");
			JsonObject fields = element.getJsonObject("fields");
			for (String name : fileElementNames) {
				JsonObject fieldValue = fields.getJsonObject(name);
				if (fieldValue != null) {
					downloadables.add(new Downloadable(uuid, name));
				}
			}
		}

		return Single.just(downloadables);
	}

	/**
	 * Download all binaries from the list of downloadables and add to a zip file
	 *
	 * @param projectName project name
	 * @param downloadables list of downloadables
	 * @return single zip file
	 * @throws FileNotFoundException
	 */
	protected Single<File> zipBinaries(String projectName, List<Downloadable> downloadables) throws FileNotFoundException {
		File zipFile = new File(System.getProperty("java.io.tmpdir"), UUIDUtil.randomUUID() + ".zip");
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

		if (downloadables.isEmpty()) {
			try {
				out.close();
				return Single.just(zipFile);
			} catch (IOException e) {
				return Single.error(e);
			}
		}

		return Observable.fromIterable(downloadables).flatMap(d -> {
			return adminClient.downloadBinaryField(projectName, d.uuid, "en", d.fieldName).toObservable().doAfterNext(MeshBinaryResponse::close).flatMap(download -> {
				addZipEntryWithUniqueFolderStructure(out, download.getFilename(), d.uuid, d.fieldName);
				try (InputStream ins = download.getStream()) {
					IOUtils.copy(ins, out);
				}
				out.closeEntry();
				out.flush();
				return Observable.just(zipFile);
			});
		}, 1).doOnError(t -> {
			out.close();
			zipFile.delete();
		}).doOnComplete(() -> out.close()).lastOrError();
	}

	/**
	 * Add a new ZipEntry to the ZipOutputStream instance at a given folder structure that corresponds to the form UUID-field path. Both form parameters have to be either empty or filled.
	 *
	 * @param out zip output stream
	 * @param filename proposed filename
	 * @param formDataUuid form data UUID, nullable
	 * @param fieldName form field name, nullable
	 * @throws IOException
	 */
	protected void addZipEntryWithUniqueFolderStructure(ZipOutputStream out, String filename, String formDataUuid, String fieldName) throws IOException {
		boolean tryAgain = false;
		do {
			tryAgain = false;
			StringBuilder folderName = new StringBuilder();
			if (org.apache.commons.lang.StringUtils.isNotBlank(formDataUuid) && org.apache.commons.lang.StringUtils.isNotBlank(fieldName)) {
				folderName.append(formDataUuid);
				folderName.append("/");
				folderName.append(fieldName);
				folderName.append("/");
			} else if (org.apache.commons.lang.StringUtils.isNotBlank(formDataUuid) ^ org.apache.commons.lang.StringUtils.isNotBlank(fieldName)) {
				throw new IllegalStateException("Incomplete form data: form UUID: " + formDataUuid + ", field: " + fieldName);
			}
			out.putNextEntry(new ZipEntry(folderName + filename));
		} while (tryAgain);
	}

	/**
	 * Get the base URL for binary field downloads.
	 *
	 * <p>
	 * The base URL must be a string such that
	 * {@code getDownloadBaseUrl(form, project) + "/:formDataUuid/binary/:binaryFieldName"} is a valid download URL.
	 * </p>
	 *
	 * <p>
	 * When no {@code downloadBaseUrl} is specified in the form, the URL will be the binary download endpoint for the
	 * forms plugin itself, otherwise the provied {@code downloadBaseUrl} is used.
	 * </p>
	 *
	 * <p>
	 * <strong>Important:</strong> Note that the form UUID must be part of
	 * the string generated by {@code getDownloadBaseUrl()}.
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
				GraphQLRequest graphQlRequest = getDataGraphQlRequest(form, query, pagingParams, configuration.isForceNoES());
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
	 * Create a GraphQL request to load form data nodes for the specified form. Loads the specified form and creates a
	 * GraphQLRequest with a query to find the form data nodes (and the page) indicated by the HTTP query string.
	 *
	 * @param form The form to load form data for
	 * @param query The Elasticsearch query to use
	 * @param pagingParams Paging parameters for the GraphQL query
	 * @return a GraphQLRequest
	 * @throws IOException if loading the ElasticSearch or GraphQL query fails
	 */
	private GraphQLRequest getDataGraphQlRequest(Form form, String query, JsonObject pagingParams, boolean forceNoES) throws IOException {
		JsonObject graphQLParams = new JsonObject(pagingParams.toBuffer());
		Map<String, String> context = new HashMap<>();

		String rawGraphQLQuery;
		if (StringUtils.isNotBlank(query) && !forceNoES) {
			rawGraphQLQuery = IOUtils.toString(FormsPlugin.class.getResource("findFormData.graphql"), StandardCharsets.UTF_8);
			graphQLParams.put("query", assembleFindFormDataESQuery(form.getUuid(), query).toString());
		} else {
			rawGraphQLQuery = IOUtils.toString(FormsPlugin.class.getResource("findFormDataNoES.graphql"), StandardCharsets.UTF_8);
			if (pagingParams.containsKey("sortBy")) {
				SortOrder order = SortOrder.valueOf(Objects.toString(pagingParams.getValue("sortOrder"))) ;
				String sortBy = pagingParams.getString("sortBy").replace(" ", "_");
				if (order == null || order == SortOrder.UNSORTED) {
					order = SortOrder.ASCENDING ;
				}
				context.put("sorting", "sort: { " + sortBy + ": " + order.name() + " }");
			} else if (configuration.getDefaultSortOrder() != null && StringUtils.isNotEmpty(configuration.getDefaultSortBy())) {
				context.put("sorting", "sort: { " + configuration.getDefaultSortBy().replace(" ", "_") + ": " + configuration.getDefaultSortOrder().name() + " }");
			} else {
				context.put("sorting", StringUtils.EMPTY);
			}
			if (forceNoES && StringUtils.isNotBlank(query)) {
				String filter = form.getElements().stream().map(element -> {
					if (element.isMultivalue()) {
						return element.getGraphQLName() + ": {anyMatch: {like: \"%" + query + "%\"}}";
					} else {
						return element.getGraphQLName() + ": {like: \"%" + query + "%\"}";
					}
				}).collect(Collectors.joining(" ", " fields: {" + SchemaService.getSchemaName(form.getUuid()) + ":{or: { ", " }}} "));
				context.put("fieldFilter", filter);
			} else {
				context.put("fieldFilter", StringUtils.EMPTY);
			}			
		}
		context.put("schemaName", SchemaService.getSchemaName(form.getUuid()));
		context.put("fieldNames", String.join("\n", form.getReportableElementNamesForGraphQL()));

		String graphQLQuery = new Handlebars().compileInline(rawGraphQLQuery).apply(Context.newContext(context));

		return new GraphQLRequest().setQuery(graphQLQuery).setVariables(graphQLParams);
	}

	/**
	 * Gets the binary field for the specified form data node and sends it to the meshClient.
	 *
	 * @param formUuid the UUID of the form node
	 * @param formDataUuid the UUID of the data node
	 * @param binaryFieldName the name of the binary field in the form data node
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
	 * @param projectName project name
	 * @param formUuid the UUID of the form node to be deleted
	 * @param formDataUuid the UUID of the data node
	 * @return single void
	 */
	public Single<EmptyResponse> delete(String projectName, String formUuid, String formDataUuid) {
		return load(projectName, formUuid, formDataUuid, true).flatMap(formDataNode -> {
			// loadFormDataByUuid() already checks if formDataUuid is a subnode of formUuid and if formUuid is actually a form.
			DeleteParametersImpl deleteParams = new DeleteParametersImpl();
			deleteParams.setRecursive(true);
			return userClient.deleteNode(projectName, formDataUuid, deleteParams).toSingle();
		});
	}

	/**
	 * Render the form with data
	 *
	 * @param projectName project name
	 * @param formUuid form UUID
	 * @param formDataInfo form data UUID and optional form page element name (separated by comma)
	 * @param rc routing context
	 * @param lang optional languages
	 * @return single buffer of rendered form
	 */
	public Single<Buffer> render(String projectName, String formUuid, String formDataInfo, RoutingContext rc, String... lang) {
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
				load(projectName, formUuid, formDataUuid),
				(formNode, formDataNode) -> {
					Form form = formService.toForm(formNode.getFields().getStringField("json").getString(), formNode.getUuid(), formNode.getLanguage())
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
	private Single<PostDataResponse> savePostedData(FormDataCreateContext ctx) {
		Form form = ctx.getForm();
		MultiMap postedData = ctx.getPostedData();
		Single<Map<String, ValidationError>> errorSingle = Single.zip(
				Single.just(form.preventInjection(postedData, configuration.getSecurityOptions())), 
				form.validate(postedData, ctx.getFileUploads(), ctx.getFormDataNode(), rxVertx, okHttpClientService.getClient()), 
				(a,b) -> {
					HashMap<String, ValidationError> errors = new HashMap<>();
					errors.putAll(a);
					errors.putAll(b);
					return errors;
				});

		return errorSingle.flatMap(errors1 -> {
			// check whether we have any validation errors, that prevent storing of the data
			Optional<ValidationError> errorPreventingDataStorage = errors1.values().stream()
					.filter(ValidationError::isPreventStoring).findAny();

			if (errorPreventingDataStorage.isPresent()) {
				// log a warning
				log.warn("Storing of POSTed data to form {} prevented by field {}. Validation error: {}", form.getUuid(),
						errorPreventingDataStorage.get().getName(), errorPreventingDataStorage.get().getMessage());

				// just return the errors in the response, but don't save anything
				PostDataResponse response = new PostDataResponse();
				response.setErrors(errors1);
				response.setFinished(false);
				if (ctx.getReferer() != null) {
					response.setRedirectUrl(ctx.getReferer());
				} else {
					response.setRedirectUrl("");
				}

				return Single.just(response);
			} else {
				String projectName = ctx.getProjectName();
				NodeResponse formNode = ctx.getFormNode();

				Single<String> dataFolderUuidSingle = ParameterUtil.getLanguages(ctx.getContext())
						.flatMap(languages -> getFormDataFolderUuid(projectName, formNode.getUuid(), languages));

				return Single.just(errors1).zipWith(
						dataFolderUuidSingle, 
						(errors, dataFolderUuid) -> saveFormData(projectName, formNode, dataFolderUuid, ctx.getFormDataNode(), form, postedData, errors).map(formData -> Pair.of(formData, errors)))
					.flatMap(single -> single)
					.flatMap(savedFormDataNodePlusErrors -> saveFileUploads(ctx.getProjectName(), form, savedFormDataNodePlusErrors.getKey(), postedData, ctx.getFileUploads()).map(uploads -> Pair.of(uploads, savedFormDataNodePlusErrors.getValue())))
					.flatMap(savedFileUploadsPlusErrors -> {
						NodeResponse savedFileUploads = savedFileUploadsPlusErrors.getKey();
						Map<String, ValidationError> errors = savedFileUploadsPlusErrors.getValue();
						if (ctx.isPublish() && errors.isEmpty() && form.isCompleted(savedFileUploads)) {
							return publishFormData(ctx, savedFileUploads, errors);
						} else {
							// set the data cookie
							if (ctx.handleCookies()) {
								Cookie cookie = Cookie.cookie(COOKIE_NAME_DATA, form.getDataCookie(savedFileUploads.getUuid()));
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
		});
	}

	/**
	 * Get the uuid of the form data folder as single
	 * @param projectName project name
	 * @param formUuid form UUID
	 * @param languages languages
	 * @return
	 */
	private Single<String> getFormDataFolderUuid(String projectName, String formUuid, String... languages) {
		return formService.getFormFolderUuid(projectName, formUuid, languages, FormService.DATA_FOLDER_NAME);
	}

	/**
	 * Save the form data to mesh
	 *
	 * @param projectName project name
	 * @param formNode form node
	 * @param dataFolderUuid uuid of the data folder
	 * @param formDataNode optional form data node (for updating)
	 * @param form form object
	 * @param postedData posted data
	 * @param errors map containing validation errors
	 * @return single of the node response
	 */
	private Single<NodeResponse> saveFormData(String projectName, NodeResponse formNode, String dataFolderUuid, NodeResponse formDataNode, Form form,
		MultiMap postedData, Map<String, ValidationError> errors) {
		FieldContainer container = null;
		NodeCreateRequest createRequest = null;
		NodeUpdateRequest updateRequest = null;

		if (formDataNode == null) {
			createRequest = new NodeCreateRequest().setSchemaName(SchemaService.getSchemaName(formNode.getUuid()))
				.setParentNodeUuid(dataFolderUuid)
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
	 * @param projectName project name
	 * @param form saved form
	 * @param formDataNode form data node
	 * @param postedData posted form data
	 * @param fileUploads validated file uploads
	 * @return single of the form data node
	 */
	private Single<NodeResponse> saveFileUploads(String projectName, Form form, NodeResponse formDataNode, MultiMap postedData, Set<FileUpload> fileUploads) {
		Set<String> validUploadElements = form.flattened()
			.filter(FormElement::isActive)
			.flatMap(elem -> elem instanceof FileElement && !elem.skipElement(postedData)
				? Stream.of(elem.getName())
				: Stream.empty())
			.collect(Collectors.toSet());

		fileUploads = fileUploads.stream().filter(fileUpload -> validUploadElements.contains(fileUpload.name())).collect(Collectors.toSet());

		if (fileUploads.isEmpty()) {
			return Single.just(formDataNode);
		}

		return Flowable.fromIterable(fileUploads)
			// The upload can be blocking in some implementations of the rest client. Therefore we queue
			// it as a worker job just in case.
			.flatMapSingle(fileUpload -> rxVertx.<NodeResponse>rxExecuteBlocking(prm -> {
				long size = fileUpload.size();
				InputStream ins;
				try {
					ins = new FileInputStream(fileUpload.uploadedFileName());
				} catch (FileNotFoundException e) {
					prm.fail(e);
					return;
				}

				// Always use the draft version because updating the binary field will increase the version of the node,
				// which can lead to problems when multiple files are uploaded.
				adminClient.updateNodeBinaryField(projectName, formDataNode.getUuid(), formDataNode.getLanguage(), "draft",
						fileUpload.name(),
						ins, size, fileUpload.fileName(), fileUpload.contentType())
					.toSingle().subscribe(prm::complete, prm::fail);
			}).toSingle(), false, 1)
			.lastOrError();
	}

	/**
	 * Publish the form data. This will also send mails to the configured users, remove the data cookie and create a
	 * response.
	 *
	 * @param ctx Request context
	 * @param savedFileUploads saved file uploads from {@link #saveFileUploads(String,Form, NodeResponse, MultiMap, Set)}
	 * @param errors
	 * @return single
	 */
	private Single<PostDataResponse> publishFormData(FormDataCreateContext ctx, NodeResponse savedFileUploads, Map<String, ValidationError> errors) {
		return adminClient.publishNode(ctx.getProjectName(), savedFileUploads.getUuid()).toSingle().flatMap(publishStatus -> {
			PluginContext meshContext = ctx.getContext();
			Form form = ctx.getForm();
			MultiMap postedData = ctx.getPostedData();

			// send mails to admin and user
			mailService.sendMails(ctx.getProjectName(), ctx.getForm(), savedFileUploads, meshContext);

			// perform actions
			return Single.zip(
					StreamUtil.toIterable(ctx.getForm().flattened().map(e -> e.performSubmit(ctx.getForm(), postedData, rxVertx, okHttpClientService.getClient()).toSingleDefault(1))), 
					allSubmitted -> {
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
		});
	}

	/**
	 * Add or update the {@link #COOKIE_NAME_VOTED forms-voted} cookie to include the UUID of the form for which data
	 * was saved.
	 *
	 * <p>
	 * The form UUIDs in the resulting cookie are delimited by a colon ({@code :}).
	 * </p>
	 *
	 * @param ctx The current form data save context
	 */
	private void addPollCookie(FormDataCreateContext ctx) {
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
	 * @param url URL
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
	 * Creates an ElasticSearch query to find data nodes for the specified form with the search string (if any) that is
	 * contained in the HTTP query string.
	 *
	 * @param formUuid the UUID of the form
	 * @param userQuery the value of the 'q' parameter from the HTTP query string (may be null)
	 * @return the ElasticSearch query as a JsonObject
	 * @throws IOException if loading the ElasticSearch query fails
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
	 * @param form form
	 * @param formData form data
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
	 *
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

	/**
	 * Get an optional configuration from either the environment variable, or the system parameter.
	 * 
	 * TODO Maybe move off to Mesh Core / Plugin API
	 * @param <T>
	 * @param configName
	 * @param defaultValue
	 * @param parse
	 * @param stringify
	 * @return
	 */
	private static final <T> T getOptionalConfig(String configName, T defaultValue, Function<String, T> parse, Function<T, String> stringify) {
		return parse.apply(System.getenv()
				.entrySet().stream()
				.filter(e -> e.getKey().equals(configName))
				.map(e -> e.getValue())
				.filter(Objects::nonNull)
				.findAny()
				.or(() -> Optional.ofNullable(System.getProperty(configName, stringify.apply(defaultValue))))
				.orElseGet(() -> stringify.apply(defaultValue)));
	}

	/** Wrapper class for downloadable binaries. */
	protected static class Downloadable {
		String uuid;
		String fieldName;

		public Downloadable(String uuid, String fieldName) {
			this.uuid = uuid;
			this.fieldName = fieldName;
		}
	}
}
