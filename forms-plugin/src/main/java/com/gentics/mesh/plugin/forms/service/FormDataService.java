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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.image.ImageManipulationRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantsResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.util.AuthUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FileUploadOptions;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.export.ExportData;
import com.gentics.mesh.plugin.forms.export.Exporter;
import com.gentics.mesh.plugin.forms.model.BinaryElement;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.model.schema.BinaryData;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;
import com.gentics.mesh.plugin.forms.util.FormParameterUtil;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UUIDUtil;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
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

	private final ObjectMapper objectMapper;

	private final FormService formService;

	private final PagingService pagingService;

	private final MailService mailService;

	private final StatusService statusService;

	private final OkHttpClientService okHttpClientService;

	private final CaptchaService captchaService;

	private final SchemaService schemaService;

	private final File storageDir;

	private final String loadFormDataToMigrateGraphQl;

	@Inject
	public FormDataService(
			@AdminClient MeshRestClient adminClient,
			@UserClient @Nullable MeshRestClient userClient,
			io.vertx.core.Vertx vertx,
			ObjectMapper objectMapper,
			FormService formService,
			PagingService pagingService,
			MailService mailService,
			StatusService statusService,
			OkHttpClientService okHttpClientService,
			CaptchaService captchaService,
			SchemaService schemaService,
			PluginManifest manifest,
			@StorageDir File storageDir) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.rxVertx = new Vertx(vertx);
		this.objectMapper = objectMapper;
		this.formService = formService;
		this.pagingService = pagingService;
		this.mailService = mailService;
		this.statusService = statusService;
		this.okHttpClientService = okHttpClientService;
		this.captchaService = captchaService;
		this.schemaService = schemaService;
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
	 * @param formtypeConfigs formtype configs
	 * @param ctx Request context
	 * @return single of the post data response
	 */
	public Single<FormData> create(FormtypeConfigsModel formtypeConfigs, FormDataCreateContext ctx) {
		return formService.loadAsAdmin(ctx.getProjectName(), ctx.getFormUuid(), true, "en")
			.doOnSuccess(ctx::setFormNode)
			.flatMap(node -> formService.toForm(formtypeConfigs, node))
			.doOnSuccess(ctx::setForm)
			.flatMap(form -> savePostedData(formtypeConfigs, ctx));
	}

	/**
	 * Update already posted data
	 *
	 * @param formtypeConfigs formtype configs
	 * @param ctx Request context
	 * @return single of the post data response
	 */
	public Single<FormData> update(FormtypeConfigsModel formtypeConfigs, FormDataUpdateContext ctx) {
		return Single.zip(
				// always load English version of form
				formService.loadAsAdmin(ctx.getProjectName(), ctx.getFormUuid(), true, "en"),
				load(ctx.getProjectName(), ctx.getFormUuid(), ctx.getFormDataUuid()),
				(formNode, formDataNode) -> formService.toForm(formtypeConfigs, formNode)
						.flatMap(form -> {
							ctx.setFormNode(formNode);
							ctx.setFormDataNode(formDataNode);
							ctx.setForm(form);
							return savePostedData(formtypeConfigs, ctx);
						}))
				.flatMap(x -> x);
	}

	/**
	 * Validate data posted by a user for the form without saving it.
	 * 
	 * @param formtypeConfigs formtype configs
	 * @param ctx Request context
	 * @return single of form data validation state
	 */
	public Single<FormData> validate(FormtypeConfigsModel formtypeConfigs, FormDataCreateContext ctx) {
		return formService.loadAsAdminWithLanguageFallback(ctx.getProjectName(), ctx.getFormUuid(), true, "en")
			.doOnSuccess(ctx::setFormNode)
			.flatMap(node -> formService.toForm(formtypeConfigs, node))
			.doOnSuccess(ctx::setForm)
			.flatMap(form -> validatePostedData(ctx.getPostedData(), statusService.getEffectiveConfiguration(formtypeConfigs, form.getType()), ctx))
			.map(errors -> toValidationResponse(ctx, errors));
	}

	/**
	 * Validate data posted by a user for an existing form data node without saving it.
	 *
	 * @param formtypeConfigs formtype configs
	 * @param ctx Request context
	 * @return single of form data validation state
	 */
	public Single<FormData> validate(FormtypeConfigsModel formtypeConfigs, FormDataUpdateContext ctx) {
		return Single.zip(
			formService.loadAsAdminWithLanguageFallback(ctx.getProjectName(), ctx.getFormUuid(), true, "en"),
				load(ctx.getProjectName(), ctx.getFormUuid(), ctx.getFormDataUuid()),
				(formNode, formDataNode) -> formService.toForm(formtypeConfigs, formNode)
					.flatMap(form -> {
						ctx.setFormNode(formNode);
						ctx.setFormDataNode(formDataNode);
						ctx.setForm(form);
						return validatePostedData(ctx.getPostedData(), statusService.getEffectiveConfiguration(formtypeConfigs, form.getType()), ctx).map(errors -> toValidationResponse(ctx, errors));
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
	 * Load the form data by uuid with the admin client
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
	 * @param formtypeConfigs formtype configs
	 * @param projectName The project name.
	 * @param formUuid The UUID of the form to export data for.
	 * @param context The current request context.
	 * @param lang The requested languages.
	 * @return The created export node.
	 */
	public Single<NodeResponse> requestExport(FormtypeConfigsModel formtypeConfigs, String projectName, String formUuid, PluginContext context, String... lang)  {

		return formService.getFormFolderUuid(projectName, formUuid, lang, FormService.DOWNLOADS_FOLDER_NAME)
				.flatMap(folderUuid -> formService.loadForm(projectName, formUuid) // TODO 8.5 language ?
						.flatMap(form -> {
							FormsConfig configuration = statusService.getEffectiveConfiguration(formtypeConfigs, form.getString("type"));
							String options = FormParameterUtil.getExportOptions(context, configuration);

							return createDownloadNode(projectName, folderUuid, "export", "form-export",
									form.getString("name"), options);
						}))
				.flatMap(exportNode -> {
					MessageConsumer<Object> consumer = rxVertx.eventBus()
							.consumer(MESSAGE_DOWNLOAD_CHECK_PREFIX + exportNode.getUuid(), msg -> msg.reply(true));

					Single<ExportData> export = export(formtypeConfigs, projectName, formUuid, context, lang);

					if (statusService.getEffectiveConfiguration(formtypeConfigs, "").isTesting()) {
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
	 * @param formtypeConfigs formtype configs
	 * @param projectName The project name.
	 * @param formUuid The form to download binaries for.
	 * @param languages The requested languages.
	 * @return The created download node for the binary download.
	 */
	public Single<NodeResponse> requestZipBinaries(FormtypeConfigsModel formtypeConfigs, String projectName, String formUuid, String ...languages) {
		return formService.getFormFolderUuid(projectName, formUuid, languages, FormService.DOWNLOADS_FOLDER_NAME)
			.flatMap(folderUuid -> statusService.languageFallback(projectName, languages)
				.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
				.flatMap(node -> formService.toForm(formtypeConfigs, node))
				.flatMap(form -> createDownloadNode(projectName, folderUuid, "binaries", "binaries", form.getName(), "")
					.flatMap(downloadNode -> {
						MessageConsumer<Object> consumer = rxVertx.eventBus()
							.consumer(MESSAGE_DOWNLOAD_CHECK_PREFIX + downloadNode.getUuid(), msg -> msg.reply(true));

						Single<Pair<Form, File>> binaries = schemaService.getSchemaFields(projectName, formUuid).flatMap(schemaFields -> zipBinaries(projectName, form, schemaFields));

						if (statusService.getEffectiveConfiguration(formtypeConfigs, "").isTesting()) {
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
	 * @param formtypeConfigs formtype configs
	 * @param projectName project name
	 * @param formUuid The UUID of the form to export
	 * @param context The current routing context
	 * @param lang optional languages
	 * @return single export data
	 */
	public Single<ExportData> export(FormtypeConfigsModel formtypeConfigs, String projectName, String formUuid, PluginContext context, String... lang) {
		// Export only published data nodes by default.
		boolean publishedOnly = Optional.ofNullable(context.queryParams().get(PARAM_PUBLISHED_ONLY))
			.map(Boolean::parseBoolean)
			.orElse(true);

		return statusService.languageFallback(projectName, lang)
			.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
			.flatMap(node -> formService.toForm(formtypeConfigs, node))
			.flatMap(form -> {
				FormsConfig configuration = statusService.getEffectiveConfiguration(formtypeConfigs,
						form.getType());
				Exporter exporter = Exporter.create(configuration, userClient, context,
						new File(System.getProperty("java.io.tmpdir")), formUuid,
						SchemaService.getSchemaName(formUuid), configuration.getExportOptions(),
						getDownloadBaseUrl(form, projectName));

				return schemaService.getSchemaFields(projectName, formUuid).flatMap(fields -> {
					return exporter
							.setFieldLabels(form.getReportableFields())
							.export(form, publishedOnly, fields);
				});
			});
	}

	/**
	 * Load the form with the given UUID with language fallback and then create
	 * a zip file containing its uploaded binaries.
	 *
	 * @see #zipBinaries(String, Form)
	 * @param formtypeConfigs formtype configs
	 * @param projectName The project name.
	 * @param uuid The UUID of the form to zip the binaries for.
	 * @param languages The requested languages.
	 * @return A {@code Pair} containing the form and the created zip file.
	 */
	public Single<Pair<Form, File>> zipBinaries(FormtypeConfigsModel formtypeConfigs, String projectName, String uuid, String ...languages) {
		return statusService.languageFallback(projectName, languages)
			.flatMap(langFallback -> formService.load(projectName, uuid, langFallback))
			.flatMap(node -> formService.toForm(formtypeConfigs, node))
			.flatMap(form -> schemaService.getSchemaFields(projectName, uuid).flatMap(schemaFields -> zipBinaries(projectName, form, schemaFields)));
	}

	/**
	 * Create a zip file containing the uploaded binaries for the given form.
	 * @param projectName The project name.
	 * @param form The forms to zip binaries for.
	 * @param schemaFields schema fields
	 * @return A {@code Pair} containing the form and the created zip file.
	 */
	public Single<Pair<Form, File>> zipBinaries(String projectName, Form form, List<String> schemaFields) {
		Set<String> fileElementNames = form.getElementsInsideProperties().filter(elem -> elem instanceof BinaryElement).map(FormElement::getName)
			.filter(Objects::nonNull).collect(Collectors.toSet());
		Set<String> fileFieldNames = form.getElementsInsideProperties().filter(elem -> elem instanceof BinaryElement)
				.map(e -> e.getGraphQLName(schemaFields)).filter(Objects::nonNull).collect(Collectors.toSet());

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
				Object value = fields.getValue(name);
				if (value instanceof JsonObject itemObject) {
					downloadables.add(new Downloadable(itemObject.getString("uuid"), "binary", Optional.of("%s/%s".formatted(uuid, name))));
				} else if (value instanceof JsonArray listValue) {
					for (int j = 0; j < listValue.size(); j++) {
						JsonObject itemObject = listValue.getJsonObject(j);
						downloadables.add(new Downloadable(itemObject.getString("uuid"), "binary", Optional.of("%s/%s/%s/%d".formatted(uuid, itemObject.getString("uuid"), name, j))));
					}
				} else {
					// try fallback to old binary field
					String oldFieldName = FormService.OLD_BINARY_FIELD_PATTERN.formatted(name);
					value = fields.getValue(oldFieldName);
					if (value instanceof JsonObject) {
						downloadables.add(new Downloadable(uuid, oldFieldName, Optional.of("%s/%s".formatted(uuid, name))));
					}
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
				addZipEntryWithUniqueFolderStructure(out, download.getFilename(), d.uuid, d.fieldName, d.maybeFolderName);
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
	protected void addZipEntryWithUniqueFolderStructure(ZipOutputStream out, String filename, String formDataUuid, String fieldName, Optional<String> maybeFolderName) throws IOException {
		boolean tryAgain = false;
		do {
			tryAgain = false;
			StringBuilder folderName = new StringBuilder();
			if (maybeFolderName.isPresent()) {
				folderName.append(maybeFolderName.get());
				folderName.append("/");
			} else if (org.apache.commons.lang.StringUtils.isNotBlank(formDataUuid) && org.apache.commons.lang.StringUtils.isNotBlank(fieldName)) {
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
	 * @param formtypeConfigs formtype configs
	 * @param projectName project name
	 * @param formUuid the UUID of the form node
	 * @param query search query
	 * @param pagingParams paging parameters
	 * @param publishedOnly whether to include only published form data nodes
	 * @return single search response
	 */
	public Single<JsonObject> find(FormtypeConfigsModel formtypeConfigs, String projectName, String formUuid, String query, JsonObject pagingParams, boolean publishedOnly) {
		return statusService.languageFallback(projectName)
			.flatMap(langFallback -> formService.load(projectName, formUuid, langFallback))
			.flatMap(node -> formService.toForm(formtypeConfigs, node))
			.flatMap(form -> schemaService.getSchemaFields(projectName, formUuid).map(fields -> Pair.of(form, fields)))
			.flatMap(pair -> {
				Form form = pair.getLeft();
				List<String> fieldNames = pair.getRight();
				FormsConfig configuration = statusService.getEffectiveConfiguration(formtypeConfigs, form.getType());

				GraphQLRequest graphQlRequest = getDataGraphQlRequest(configuration, form, fieldNames, query, pagingParams, configuration.isForceNoES());
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
							clientResponse.put("entries", rewriteReportedEntries(form, fieldNames, nodes.getJsonArray("elements")));

							return Single.just(clientResponse.put("elements", new JsonObject(form.elementMap())));
						}

						return Single.error(new FormsError("Unexpected NULL search response.", 500));
					});
			});
	}

	/**
	 * Rewrite the entries by moving the data from the old binary fields to the new ones
	 * @param form form
	 * @param fieldNames field names in the schema
	 * @param entries entries to rewrite
	 * @return rewritten entries
	 */
	protected JsonArray rewriteReportedEntries(Form form, List<String> fieldNames, JsonArray entries) {
		List<String> binaryNamesWithOldVariants = form.getElementsInsideProperties()
			.filter(FormElement::isActive)
			.filter(elem -> elem instanceof BinaryElement)
			.map(FormElement::getName)
			.filter(name -> {
				String oldFieldName = FormService.OLD_BINARY_FIELD_PATTERN.formatted(name);
				return fieldNames.contains(oldFieldName);
			})
			.collect(Collectors.toList());

		if (binaryNamesWithOldVariants.isEmpty()) {
			return entries;
		}

		for (Object entry : entries) {
			if (entry instanceof JsonObject entryObject) {
				JsonObject fields = entryObject.getJsonObject("fields");
				if (fields != null) {
					for (String fieldName : binaryNamesWithOldVariants) {
						String oldFieldName = FormService.OLD_BINARY_FIELD_PATTERN.formatted(fieldName);
						JsonObject value = fields.getJsonObject(fieldName);
						JsonObject oldValue = fields.getJsonObject(oldFieldName);
						if (value == null && oldValue != null) {
							String fileName = oldValue.getString("fileName");
							value = JsonObject.of("fields", JsonObject.of("binary", JsonObject.of("fileName", fileName)));
							fields.put(fieldName, value);
						}
						fields.remove(oldFieldName);
					}
				}
			}
		}

		return entries;
	}

	/**
	 * Create a GraphQL request to load form data nodes for the specified form. Loads the specified form and creates a
	 * GraphQLRequest with a query to find the form data nodes (and the page) indicated by the HTTP query string.
	 *
	 * @param configuration configuration
	 * @param form The form to load form data for
	 * @param fieldNames field names of the schema
	 * @param query The Elasticsearch query to use
	 * @param pagingParams Paging parameters for the GraphQL query
	 * @return a GraphQLRequest
	 * @throws IOException if loading the ElasticSearch or GraphQL query fails
	 */
	private GraphQLRequest getDataGraphQlRequest(FormsConfig configuration, Form form, List<String> fieldNames, String query, JsonObject pagingParams, boolean forceNoES) throws IOException {
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
				String filter = form.getSchema().getProperties().values().stream()
						.map(element -> element.installFilter(query))
						.collect(Collectors.joining(" ", " fields: {" + SchemaService.getSchemaName(form.getUuid()) + ":{or: { ", " }}} "));
				context.put("fieldFilter", filter);
			} else {
				context.put("fieldFilter", StringUtils.EMPTY);
			}			
		}
		context.put("schemaName", SchemaService.getSchemaName(form.getUuid()));
		context.put("fieldNames", String.join("\n", form.getReportableElementNamesForGraphQL(fieldNames)));

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
			NodeField nodeField = formDataNode.getFields().getNodeField(binaryFieldName);
			if (nodeField != null) {
				String binaryUuid = nodeField.getUuid();
				return userClient.downloadBinaryField(projectName, binaryUuid, formDataNode.getLanguage(), "binary").getResponse();
			} else {
				// fallback to the old field name
				String oldBinaryFieldName = FormService.OLD_BINARY_FIELD_PATTERN.formatted(binaryFieldName);
				BinaryField oldBinaryField = formDataNode.getFields().getBinaryField(oldBinaryFieldName);
				if (oldBinaryField != null) {
					return userClient.downloadBinaryField(projectName, formDataUuid, formDataNode.getLanguage(), oldBinaryFieldName).getResponse();
				} else {
					return Single.error(new FormsError("Error while loading binary data of field %s for form data %s of form %s".formatted(binaryFieldName, formDataUuid, formUuid), 400));
				}
			}
		});
	}

	/**
	 * Gets the multi-binary field item for the specified form data node and sends it to the meshClient.
	 *
	 * @param formUuid the UUID of the form node
	 * @param formDataUuid the UUID of the data node
	 * @param binaryFieldName the name of the binary field in the form data node
	 * @param index index of a multi-field binary
	 */
	public Single<MeshResponse<MeshBinaryResponse>> loadMultiBinaryItem(String projectName, String formUuid, String formDataUuid, String binaryFieldName, int index) {
		// Binaries can be loaded for already published form data as well, so call the variant of load() which allows loading published form data.
		return load(projectName, formUuid, formDataUuid, true).flatMap(formDataNode -> {
			try {
				NodeFieldList nodeList = formDataNode.getFields().getNodeFieldList(binaryFieldName);
				String binaryUuid = nodeList.getItems().get(index).getUuid();
				return userClient.downloadBinaryField(projectName, binaryUuid, formDataNode.getLanguage(), "binary").getResponse();
			} catch (Exception e) {
				// TODO FIXME Mesh Currently there is no way to determine a field's type from the node response, other that load the corresponding form schema.
				// Here we try deserializing the node list, which may spit random deserialization errors.
				log.error("Deserialization error", e);
			}
			return Single.error(new FormsError("Error retrieving binary [%d] of field %s for the form data %s".formatted(index, binaryFieldName, formDataUuid), 400));
		});
	}

	/**
	 * Load the binary for formgen. Will return 404 when the form does not exist, the binary does not exist or does not belong to the form or does belong to
	 * an already published form data node-
	 * @param projectName project name
	 * @param formUuid form UUID
	 * @param binaryUuid binary UUID
	 * @param width width
	 * @param height height
	 * @return single response
	 */
	public Single<MeshResponse<MeshBinaryResponse>> loadBinaryForFormGen(String projectName, String formUuid, String binaryUuid, String width, String height) {
		// load the form with uuid as admin
		Single<NodeResponse> formNodeSingle = formService.loadAsAdmin(projectName, formUuid, true);

		// load the binary with uuid as admin, check whether it is a binary item node and map to the parents
		Single<NodeResponse> binaryNodeSingle = adminClient.findNodeByUuid(projectName, binaryUuid, new NodeParametersImpl()).toSingle().flatMap(nodeResponse -> {
			// check whether the node is a binary item
			if (isBinaryItemNode(nodeResponse)) {
				return Single.just(nodeResponse);
			} else {
				String message = String.format("Node with uuid \"%s\" is not a binary item node.", binaryUuid);
				return Single.error(new FormsError(message, HttpResponseStatus.NOT_FOUND.code()));
			}
		}).flatMap(nodeResponse -> {
			String formDataUuid = nodeResponse.getParentNode().getUuid();
			// check whether the immediate parent (which is the form data node) is published
			return load(projectName, formUuid, formDataUuid, false).map(ignored -> nodeResponse);
		});

		// with both the formnode and binarynode, check for consistency and return binary download
		io.reactivex.functions.BiFunction<NodeResponse, NodeResponse, Single<MeshResponse<MeshBinaryResponse>>> zipper = (formNode, binaryNode) -> {
			// check whether the form node is included in the breadcrumb
			boolean formNodeIsParent = Optional.ofNullable(binaryNode.getBreadcrumb())
					.flatMap(breadcrumb -> breadcrumb.stream()
							.filter(ref -> Strings.CI.equals(ref.getUuid(), formNode.getUuid())).findFirst())
					.isPresent();

			if (formNodeIsParent) {
				return statusService.getFormtypeConfigs(projectName, adminClient)
					.flatMap(config -> formService.toForm(config, formNode))
					.map(form -> getThumbnailInfo(form.getConfiguration()))
					.flatMap(thumbnails -> {
						ImageManipulationParameters thumbnailParams = null;
						if (thumbnails.getLeft()) {
							thumbnailParams = new ImageManipulationParametersImpl().setWidth(width).setHeight(height);
						}
						// download the binary data
						return adminClient.downloadBinaryField(projectName, binaryNode.getUuid(), formNode.getLanguage(),
								"binary", thumbnailParams)
								.getResponse();
					});
			} else {
				String message = "Node with uuid \"%s\" does not belong to form \"%s\"".formatted(binaryNode.getUuid(), formNode.getUuid());
				return Single.error(new FormsError(message, HttpResponseStatus.NOT_FOUND.code()));
			}
		};

		return Single.zip(formNodeSingle, binaryNodeSingle, zipper).flatMap(single -> single);
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
	 * Perform "cancel" for the given form data node. This will load the form data node (with the admin client), but only allow draft nodes.
	 * If the node is already published, a 404 error will be thrown.
	 * The the node is deleted with the admin client (recursively)
	 * @param projectName project name
	 * @param formUuid form UUID
	 * @param formDataUuid form data UUID
	 * @return empty response
	 */
	public Single<EmptyResponse> cancel(String projectName, String formUuid, String formDataUuid) {
		return load(projectName, formUuid, formDataUuid, false).flatMap(formDataNode -> {
			DeleteParametersImpl deleteParams = new DeleteParametersImpl();
			deleteParams.setRecursive(true);
			return adminClient.deleteNode(projectName, formDataUuid, deleteParams).toSingle();
		});
	}

	/**
	 * Remove the data cookie
	 * @param ctx context
	 */
	public void removeDataCookie(PluginContext ctx) {
		ctx.response().removeCookie(COOKIE_NAME_DATA);
		Cookie cookie = Cookie.cookie(COOKIE_NAME_DATA, "");
		cookie.setPath(getPath(ctx.request().getHeader("Referer")));
		cookie.setHttpOnly(false);
		cookie.setMaxAge(0);
		ctx.response().addCookie(cookie);
	}

	/**
	 * Validate and save the data to mesh. publish form, if completed. Redirect to success url or referer
	 *
	 * @param formtypeConfigs formtype configs
	 * @param ctx
	 * @return single redirect URL
	 */
	private Single<FormData> savePostedData(FormtypeConfigsModel formtypeConfigs, FormDataCreateContext ctx) {
		Form form = ctx.getForm();
		PostedData postedData = ctx.getPostedData();

		FormsConfig configuration = statusService.getEffectiveConfiguration(formtypeConfigs, form.getType());

		return captchaService.validate(configuration, ctx).andThen(validatePostedData(postedData, configuration, ctx).flatMap(errors1 -> {
			// check whether we have any validation errors, that prevent storing of the data
			Optional<ValidationError> errorPreventingDataStorage = errors1.values().stream()
					.filter(ValidationError::isPreventStoring).findAny();

			if (errorPreventingDataStorage.isPresent()) {
				log.warn("Storing of POSTed data to form {} prevented by field {}. Validation error: {}", form.getUuid(),
						errorPreventingDataStorage.get().getName(), errorPreventingDataStorage.get().getMessage());

				FormData formData = toValidationResponse(ctx, errors1);
				if (ctx.getReferer() != null) {
					formData.setRedirectUrl(ctx.getReferer());
				} else {
					formData.setRedirectUrl("");
				}
				return Single.just(formData);
			} else {
				String projectName = ctx.getProjectName();
				NodeResponse formNode = ctx.getFormNode();

				Single<String> dataFolderUuidSingle = getFormDataFolderUuid(projectName, formNode.getUuid(), "en");

				return Single.just(errors1).zipWith(
						dataFolderUuidSingle, 
						(errors, dataFolderUuid) -> saveFormData(projectName, formNode, dataFolderUuid, ctx.getFormDataNode(), form, postedData, errors).map(formData -> Pair.of(formData, errors)))
					.flatMap(single -> single)
					.flatMap(savedFormDataNodePlusErrors -> saveFileUploads(configuration, ctx.getProjectName(), form, savedFormDataNodePlusErrors.getKey(), postedData, ctx.getFileUploads()).map(uploads -> Pair.of(uploads, savedFormDataNodePlusErrors.getValue())))
					.flatMap(savedFileUploadsPlusErrors -> {
						NodeResponse savedFileUploads = savedFileUploadsPlusErrors.getKey();
						Map<String, ValidationError> errors = savedFileUploadsPlusErrors.getValue();
						if (ctx.isPublish() && errors.isEmpty() && form.isCompleted(savedFileUploads)) {
							return publishFormData(formtypeConfigs, ctx, savedFileUploads, errors);
						} else {
							// set the data cookie
							if (ctx.handleCookies()) {
								Cookie cookie = Cookie.cookie(COOKIE_NAME_DATA, form.getDataCookie(savedFileUploads.getUuid()));
								cookie.setPath(getPath(ctx.getReferer()));
								cookie.setHttpOnly(false);
								ctx.getContext().response().addCookie(cookie);
							}

							ctx.setFormDataNode(savedFileUploads); // updated formdata node
							var response = toValidationResponse(ctx, errors);
							response.setFormDataID(savedFileUploads.getUuid());
							if (ctx.getReferer() != null) {
								response.setRedirectUrl(ctx.getReferer());
							} else {
								response.setRedirectUrl("");
							}
							return Single.just(response);
						}
					});
			}
		}));
	}

	/**
	 * Validate posted form data with the same rules used before saving.
	 * @param postedData 
	 *
	 * @param configuration configuration
	 * @param ctx Request context with the loaded form
	 *
	 * @return single of validation errors keyed by form element name
	 */
	private Single<Map<String, ValidationError>> validatePostedData(PostedData postedData, FormsConfig configuration, FormDataCreateContext ctx) {
		Form form = ctx.getForm();
		return Single.zip(
				Single.just(form.preventInjection(postedData, ctx, configuration.getSecurityOptions())),
				form.validate(ctx, rxVertx, okHttpClientService.getClient()),
				(a, b) -> {
					HashMap<String, ValidationError> errors = new HashMap<>();
					errors.putAll(a);
					errors.putAll(b);
					return errors;
				});
	}

	/**
	 * Create a form data response containing the validation state.
	 *
	 * @param errors validation errors keyed by form element name
	 * @return form data response
	 */
	private FormData toValidationResponse(FormDataCreateContext ctx, Map<String, ValidationError> errors) {
		var form = ctx.getForm();
		var pluginContext = ctx.getContext();
		var lang = ParameterUtil.getQueryParam(pluginContext, "lang");
		var formDataNode = ctx.getFormDataNode();
		var response = formDataNode == null
			? FormService.buildDefaultFormData(ctx.getPostedData(), form, form.getUuid(), errors, lang, AuthUtil.decodeToken(pluginContext).getMap())
			: FormService.buildFormDataFromNode(ctx.getPostedData(), form, form.getUuid(), errors, lang, formDataNode);

		response.setHasValidationErrors(!errors.isEmpty());
		return response;
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

	//TODO 8.5 kein formpage mehr
	private Single<NodeResponse> saveFormData(String projectName, NodeResponse formNode, String dataFolderUuid, NodeResponse formDataNode, Form form,
			PostedData postedData, Map<String, ValidationError> errors) {
		FieldContainer container = null;
		NodeCreateRequest createRequest = null;
		NodeUpdateRequest updateRequest = null;

		if (formDataNode == null) {
			createRequest = new NodeCreateRequest().setSchemaName(SchemaService.getSchemaName(formNode.getUuid()))
				.setParentNodeUuid(dataFolderUuid)
				.setLanguage(form.getLanguage());
			container = createRequest;
		} else {
			updateRequest = new NodeUpdateRequest();
			updateRequest.setVersion(formDataNode.getVersion());
			updateRequest.setLanguage(form.getLanguage());
			container = updateRequest;
		}

		form.addFields(container, postedData, !errors.isEmpty());

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
			return adminClient.createNode(projectName, createRequest, new NodeParametersImpl().setLanguages(form.getLanguage())).toSingle();
		} else {
			return adminClient.updateNode(projectName, formDataNode.getUuid(), updateRequest, new NodeParametersImpl().setLanguages(form.getLanguage())).toSingle();
		}
	}

	/**
	 * Save fileuploads to mesh
	 *
	 * @param configuration effective configuration
	 * @param projectName project name
	 * @param form saved form
	 * @param formDataNode form data node
	 * @param postedData posted form data
	 * @param sortedFileUploads validated file uploads
	 * @return single of the form data node
	 */
	private Single<NodeResponse> saveFileUploads(FormsConfig configuration, String projectName, Form form, NodeResponse formDataNode, PostedData postedData, Set<FileUpload> fileUploads) {
		Function<Object, BinaryData> binaryDataExtractor = value -> {
			return JsonUtil.getMapper().convertValue(value, BinaryData.class);
		};

		Triple<Boolean, Integer, Integer> thumbnails = getThumbnailInfo(configuration);

		// Reusable list item uploader
		BiFunction<Triple<String, Integer, BinaryData>, FileUpload, Single<Triple<String, Integer, BinaryData>>> listItemUploader = (key, fileUpload) -> {
			String fieldName = key.getLeft();

			NodeCreateRequest itemCreate = new NodeCreateRequest();
			itemCreate.setParentNode(new NodeReference().setProjectName(projectName).setUuid(formDataNode.getUuid()));
			itemCreate.setLanguage(formDataNode.getLanguage());
			itemCreate.setSchemaName(SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME);
			itemCreate.getFields().put("name", new StringFieldImpl().setString(fieldName));
			try {
				long size = fileUpload.size();
				InputStream ins = new FileInputStream(fileUpload.uploadedFileName());
				// Create a binary node
				return adminClient.createNode(projectName, itemCreate).toSingle()
					// and fill it
					.flatMap(itemNode -> adminClient.updateNodeBinaryField(projectName, itemNode.getUuid(), formDataNode.getLanguage(), "draft",
						"binary",
						ins, size, fileUpload.fileName(), fileUpload.contentType())
					.toSingle()
					.flatMap(itemNode1 -> {
						if (thumbnails.getLeft() && key.getRight().isImage()) {
							// create the requested image variant. If the request fails, we log it but continue with the upload
							ImageManipulationRequest request = new ImageManipulationRequest();
							request.setDeleteOther(false);
							request.setVariants(List.of(new ImageVariantRequest().setWidth(thumbnails.getMiddle()).setHeight(thumbnails.getRight())));
							return adminClient.upsertNodeBinaryFieldImageVariants(projectName,
									itemNode1.getUuid(), "binary", request).toSingle()
									.onErrorResumeNext(e -> {
										log.warn("Creating the thumbnail for uploaded image %s failed".formatted(fileUpload.fileName()), e);
										return Single.just(new ImageVariantsResponse());
									}).map(ignored -> itemNode1);
						} else {
							return Single.just(itemNode1);
						}
					})
					.map(itemNode1 -> {
						key.getRight().setValue("mesh: %s".formatted(itemNode1.getUuid()));
						return key;
					}));
			} catch (FileNotFoundException e1) {
				String message = "Could not upload a binary for the form data %s field %s".formatted(formDataNode.getUuid(), fieldName);
				log.error(message, e1);
				return Single.error(new FormsError(message, e1, 400));
			}
		};

		// first organize file uploads
		Map<Pair<String, Integer>, FileUpload> uploadsPerField = new HashMap<>();
		for (FileUpload fileUpload : fileUploads) {
			BinaryElement.extractFieldId(fileUpload.name()).ifPresent(id -> {
				uploadsPerField.put(id, fileUpload);
			});
		}

		log.debug("Found %d file uploads".formatted(uploadsPerField.size()));

		// collect the binary elements, for which upload information will be handled (if present)
		Map<String, BinaryElement> validUploadElements = form.getElementsInsideProperties()
			.filter(FormElement::isActive)
			.filter(elem -> elem instanceof BinaryElement)
			.map(elem -> BinaryElement.class.cast(elem))
			.collect(Collectors.toMap(BinaryElement::getKey, Function.identity()));

		log.debug("Form contains %d valid binary elements".formatted(validUploadElements.size()));

		return Flowable.fromIterable(validUploadElements.values())
			.filter(elem -> postedData.contains(elem.getKey()))
			.flatMap(elem -> {
				String key = elem.getKey();
				if (elem.isList()) {
					AtomicInteger indexCounter = new AtomicInteger();
					return Flowable.fromIterable(postedData.getAllValues(elem.getKey()))
						.map(v -> Triple.of(key, indexCounter.getAndIncrement(), binaryDataExtractor.apply(v)));
				} else {
					Object value = postedData.getValue(elem.getKey());

					if (value != null) {
						return Flowable.just(value).map(v -> Triple.of(key, 0, binaryDataExtractor.apply(v)));
					} else {
						return Flowable.empty();
					}
				}
			})
			.map(triple -> {
				log.debug("Handle %s.%d: %s".formatted(triple.getLeft(), triple.getMiddle(), JsonUtil.toJson(triple.getRight())));

				// is there a file upload?
				Pair<String, Integer> uploadKey = Pair.of(triple.getLeft(), triple.getMiddle());
				if (uploadsPerField.containsKey(uploadKey)) {
					log.debug("Will do file upload");
					return listItemUploader.apply(triple, uploadsPerField.get(uploadKey));
				} else {
					log.debug("File already uploaded");
					return Single.just(triple);
				}
			})
			.concatMapSingle(single -> single)
			.toList()
			.flatMap(binaryList -> {
				// make sure the binary list is sorted
				binaryList.sort((a, b) -> {
					int cmp = Strings.CI.compare(a.getLeft(), b.getLeft());
					if (cmp != 0) {
						return cmp;
					}

					return Integer.compare(a.getMiddle(), b.getMiddle());
				});
				// reorganize the list into lists per field (in correct order)
				Map<String, List<BinaryData>> binaryDataPerField = new HashMap<>();
				for (Triple<String, Integer, BinaryData> triple : binaryList) {
					String fieldId = triple.getLeft();
					List<BinaryData> fieldList = binaryDataPerField.computeIfAbsent(fieldId, key -> new ArrayList<>());
					if (triple.getRight().getMeshUuid().isPresent()) {
						fieldList.add(triple.getRight());
					}
				}

				// collect mesh uuids of binary items, that need to be removed
				Set<String> toRemove = new HashSet<>();

				// prepare the update request
				NodeUpdateRequest update = formDataNode.toRequest();

				// write back info to postedData
				for (Map.Entry<String, List<BinaryData>> entry : binaryDataPerField.entrySet()) {
					String fieldId = entry.getKey();
					BinaryElement element = validUploadElements.get(fieldId);

					if (element != null) {
						List<BinaryData> binaryData = entry.getValue();
						if (element.isList()) {
							List<String> values = binaryData.stream().map(JsonUtil::toJson).collect(Collectors.toList());
							postedData.set(fieldId, values);

							List<String> newMeshUuids = binaryData.stream().map(BinaryData::getMeshUuid)
									.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

							NodeFieldList nodeFieldList = update.getFields().getNodeFieldList(fieldId);
							if (nodeFieldList == null) {
								nodeFieldList = new NodeFieldListImpl();
								update.getFields().put(fieldId, nodeFieldList);
							}

							List<String> oldMeshUuids = nodeFieldList.getItems().stream().map(NodeFieldListItem::getUuid)
									.collect(Collectors.toList());

							// everything, which was in oldMeshUuids, but is not in newMeshUuids must be removed
							toRemove.addAll(CollectionUtils.subtract(oldMeshUuids, newMeshUuids));

							nodeFieldList.setItems(newMeshUuids.stream().map(NodeFieldListItemImpl::new).collect(Collectors.toList()));
							log.debug("Set mesh UUIDS %s to field %s".formatted(newMeshUuids, fieldId));
							update.getFields().put(fieldId, nodeFieldList);

						} else {
							NodeField nodeField = update.getFields().getNodeField(fieldId);
							if (nodeField == null) {
								nodeField = new NodeFieldImpl();
								update.getFields().put(fieldId, nodeField);
							}

							if (binaryData.isEmpty()) {
								// remove the node field, if set
								if (StringUtils.isNotBlank(nodeField.getUuid())) {
									// old binary item will be removed
									toRemove.add(nodeField.getUuid());
									log.debug("Remove mesh UUID from field %s".formatted(fieldId));
									update.getFields().put(fieldId, null);
								}
							} else {
								String newMeshUuid = binaryData.get(0).getMeshUuid().get();
								postedData.set(fieldId, JsonUtil.toJson(binaryData.get(0)));

								// check whether the referenced uuid is different
								if (StringUtils.isNotBlank(nodeField.getUuid())) {
									String oldMeshUuid = nodeField.getUuid();

									if (!Strings.CI.equals(oldMeshUuid, newMeshUuid)) {
										toRemove.add(oldMeshUuid);
									}
								}
								log.debug("Set mesh UUID %s to field %s".formatted(newMeshUuid, fieldId));
								update.getFields().put(fieldId, new NodeFieldImpl().setUuid(newMeshUuid));
							}
						}
					}
				}

				Completable postSave = Completable.concat(
						toRemove.stream().map(uuid -> adminClient.deleteNode(projectName, uuid).toCompletable())
								.collect(Collectors.toList()));

					return adminClient.updateNode(projectName, formDataNode.getUuid(), update).toSingle()
							.flatMap(response -> postSave.toSingle(() -> response));
				});
	}

	/**
	 * Publish the form data. This will also send mails to the configured users, remove the data cookie and create a
	 * response.
	 *
	 * @param formtypeConfigs formtype configs
	 * @param ctx Request context
	 * @param savedFileUploads saved file uploads from {@link #saveFileUploads(String,Form, NodeResponse, PostedData, Set)}
	 * @param errors
	 * @return single
	 */
	private Single<FormData> publishFormData(FormtypeConfigsModel formtypeConfigs, FormDataCreateContext ctx, NodeResponse savedFileUploads, Map<String, ValidationError> errors) {
		return adminClient.publishNode(ctx.getProjectName(), savedFileUploads.getUuid()).toSingle().flatMap(publishStatus -> {
			PluginContext meshContext = ctx.getContext();
			Form form = ctx.getForm();
			PostedData postedData = ctx.getPostedData();

			// send mails to admin and user
			mailService.sendMails(formtypeConfigs, ctx.getProjectName(), ctx.getForm(), savedFileUploads, meshContext);

			// perform actions
			return Single.zip(
					StreamUtil.toIterable(ctx.getForm().getElementsInsideProperties().map(e -> e.performSubmit(ctx.getForm(), postedData, rxVertx, okHttpClientService.getClient()).toSingleDefault(1))), 
					allSubmitted -> {
						// TODO: this is actually wrong, we must not check the form type
						boolean isPoll = Strings.CI.equals(ctx.getForm().getType(), "poll");

						// remove the data cookie
						if (ctx.handleCookies()) {
							removeDataCookie(meshContext);

							if (isPoll) {
								addPollCookie(ctx);
							}
						}

						FormData response = new FormData();
						response.setFormDataID(savedFileUploads.getUuid());
						response.setHasValidationErrors(!errors.isEmpty());
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
		String votedPolls = Optional.ofNullable(pc.request().getCookie(COOKIE_NAME_VOTED))
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
			.setHttpOnly(false)
			.setMaxAge(Integer.MAX_VALUE);

		pc.response().addCookie(cookie);
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
	 * Check whether the node is a form node (uses the form schema)
	 *
	 * @param node
	 *            node to check
	 * @return true for form nodes
	 */
	private boolean isBinaryItemNode(NodeResponse node) {
		return SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME.equals(node.getSchema().getName());
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

	/**
	 * Get the thumbnail info as triple (flag, width, height)
	 * @param configuration configuration
	 * @return thumbnail info
	 */
	private static Triple<Boolean, Integer, Integer> getThumbnailInfo(FormsConfig configuration) {
		AtomicBoolean createThumbnails = new AtomicBoolean(FileUploadOptions.DEFAULT_CREATE_THUMBNAILS);
		AtomicInteger thumbnailsWidth = new AtomicInteger(FileUploadOptions.DEFAULT_THUMBNAIL_WIDTH);
		AtomicInteger thumbnailsHeight = new AtomicInteger(FileUploadOptions.DEFAULT_THUMBNAIL_HEIGHT);
		Optional.ofNullable(configuration.getFileUpload()).ifPresent(fileUploadOptions -> {
			createThumbnails.set(fileUploadOptions.isCreateThumbnails());
			thumbnailsWidth.set(fileUploadOptions.getThumbnailWidth());
			thumbnailsHeight.set(fileUploadOptions.getThumbnailHeight());
		});

		// make sure the config is consistent
		if (createThumbnails.get() && (thumbnailsWidth.get() <= 0 || thumbnailsHeight.get() <= 0)) {
			log.warn(
					"Creating thumbnails is activated, but width (%d) and height (%d) are not both positive numbers. Thumbnails will not be created or returned"
							.formatted(thumbnailsWidth.get(), thumbnailsHeight.get()));
			createThumbnails.set(false);
		}

		return Triple.of(createThumbnails.get(), thumbnailsWidth.get(), thumbnailsHeight.get());
	}

	/** Wrapper class for downloadable binaries. */
	protected static class Downloadable {
		String uuid;
		String fieldName;
		Optional<String> maybeFolderName;

		public Downloadable(String uuid, String fieldName) {
			this(uuid, fieldName, Optional.empty());
		}

		public Downloadable(String uuid, String fieldName, Optional<String> maybeFolderName) {
			this.uuid = uuid;
			this.fieldName = fieldName;
			this.maybeFolderName = maybeFolderName;
		}
	}
}
