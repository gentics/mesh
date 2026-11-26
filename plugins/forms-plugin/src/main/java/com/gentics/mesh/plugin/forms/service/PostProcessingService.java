package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.PostProcessingOptions;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;
import com.gentics.mesh.plugin.forms.postprocessing.PostProcessingAction;
import com.gentics.mesh.plugin.forms.postprocessing.PostProcessingRegistry;
import com.gentics.mesh.plugin.forms.postprocessing.PostProcessingStatus;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

/**
 * Service that handles the post processing of published form data.
 *
 * <p>When form data is published, {@link #prepare(String, Form, NodeResponse, String)} checks, which of the registered
 * {@link PostProcessingAction}s need to be performed and creates a status node (schema {@link SchemaInitializer#POSTPROCESSING_SCHEMA_NAME})
 * for each of them as child of the form data node. After publishing, {@link #executeAsync(String, Form, NodeResponse, String)} performs the actions asynchronously.
 * A periodic job (started with {@link #initialize()}) retries failed actions and resumes actions, which were interrupted (e.g. by a restart).
 */
@Singleton
public class PostProcessingService {
	/**
	 * Name of the field containing the action key
	 */
	public static final String FIELD_KEY = "key";

	/**
	 * Name of the field containing the {@link PostProcessingStatus}
	 */
	public static final String FIELD_STATUS = "status";

	/**
	 * Name of the field containing the number of performed attempts
	 */
	public static final String FIELD_ATTEMPTS = "attempts";

	/**
	 * Name of the field containing the language of the posted data
	 */
	public static final String FIELD_LANGUAGE = "language";

	/**
	 * Name of the field containing the error message of the last failed attempt
	 */
	public static final String FIELD_ERROR = "error";

	/**
	 * Name of the field containing the timestamp of the last status change
	 */
	public static final String FIELD_LAST_UPDATE = "lastUpdate";

	/**
	 * Actions which are in status {@link PostProcessingStatus#RUNNING} for longer than this are considered to be interrupted.
	 */
	public static final Duration RUNNING_TIMEOUT = Duration.ofHours(1);

	/**
	 * Language of the form data nodes (and the status nodes)
	 */
	private static final String NODE_LANGUAGE = "en";

	/**
	 * Maximum number of status nodes handled in a single run of the periodic job (per project)
	 */
	private static final int RECOVERY_BATCH_SIZE = 1000;

	private static final Logger log = LoggerFactory.getLogger(PostProcessingService.class);

	private final MeshRestClient adminClient;

	private final Vertx rxVertx;

	private final FormsConfig configuration;

	private final PostProcessingRegistry registry;

	private final ObjectMapper objectMapper;

	private final FormService formService;

	private final StatusService statusService;

	private final PurgeService purgeService;

	private final String findPendingGraphQl;

	private final AtomicBoolean recoveryRunning = new AtomicBoolean(false);

	@Inject
	public PostProcessingService(@AdminClient MeshRestClient adminClient, io.vertx.core.Vertx vertx, FormsConfig configuration,
			PostProcessingRegistry registry, ObjectMapper objectMapper, FormService formService, StatusService statusService,
			PurgeService purgeService) {
		this.adminClient = adminClient;
		this.rxVertx = Vertx.newInstance(vertx);
		this.configuration = configuration;
		this.registry = registry;
		this.objectMapper = objectMapper;
		this.formService = formService;
		this.statusService = statusService;
		this.purgeService = purgeService;

		try {
			this.findPendingGraphQl = IOUtils.toString(FormsPlugin.class.getResource("findPostProcessingPending.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the service. This will schedule the periodic job, which retries failed actions and resumes interrupted actions.
	 * @return completable
	 */
	public Completable initialize() {
		return Completable.fromRunnable(() -> {
			int retryInterval = configuration.getPostProcessingOptions().getRetryInterval();
			if (registry.getActions().isEmpty()) {
				log.info("No post processing actions found, not scheduling the post processing job");
			} else if (retryInterval <= 0) {
				log.warn("Post processing retry interval is less than 1, retrying and resuming post processing actions is disabled");
			} else {
				log.info("Scheduling post processing job every {} seconds", retryInterval);
				rxVertx.setPeriodic(Duration.ofSeconds(retryInterval).toMillis(), timerId -> runRecovery().subscribe());
			}
		});
	}

	/**
	 * Check which post processing actions need to be performed for the form data node and create a status node
	 * (with status {@link PostProcessingStatus#PENDING}) for each of them.
	 * This must be called before the form data node is published.
	 *
	 * @param projectName project name
	 * @param form form
	 * @param formDataNode saved form data node
	 * @param language language of the posted data
	 * @return completable
	 */
	public Completable prepare(String projectName, Form form, NodeResponse formDataNode, String language) {
		Map<String, PostProcessingAction> actions = registry.getActions();
		if (actions.isEmpty()) {
			return Completable.complete();
		}

		return Single.fromCallable(() -> getPostedData(form, formDataNode, language)).flatMapCompletable(postedData -> {
			PostProcessingOptions options = getOptions(form);
			List<String> keys = actions.entrySet().stream().filter(entry -> {
				String key = entry.getKey();
				try {
					return entry.getValue().shouldProcess(options.getProperties(key), form, postedData, language);
				} catch (Throwable e) {
					log.error(String.format("Error while checking post processing action %s for form data %s, skipping it", key, formDataNode.getUuid()), e);
					return false;
				}
			}).map(Map.Entry::getKey).collect(Collectors.toList());

			if (keys.isEmpty()) {
				return Completable.complete();
			}

			// don't create status nodes again for actions, which already have one (could happen, if publishing failed before)
			return loadStatusNodes(projectName, formDataNode.getUuid()).flatMapCompletable(existing -> {
				Set<String> existingKeys = existing.stream().map(node -> getString(node, FIELD_KEY)).collect(Collectors.toSet());
				return Observable.fromIterable(keys).filter(key -> !existingKeys.contains(key))
						.concatMapCompletable(key -> createStatusNode(projectName, formDataNode.getUuid(), key, language));
			});
		});
	}

	/**
	 * Asynchronously perform the pending post processing actions for the form data node
	 * @param projectName project name
	 * @param form form
	 * @param formDataNode form data node
	 * @param language language of the posted data
	 */
	public void executeAsync(String projectName, Form form, NodeResponse formDataNode, String language) {
		if (registry.getActions().isEmpty()) {
			return;
		}

		loadStatusNodes(projectName, formDataNode.getUuid()).flatMapCompletable(statusNodes -> {
			List<NodeResponse> pending = statusNodes.stream()
					.filter(node -> getStatus(node) == PostProcessingStatus.PENDING)
					.collect(Collectors.toList());
			if (pending.isEmpty()) {
				return Completable.complete();
			}
			PostedData postedData = getPostedData(form, formDataNode, language);
			return Observable.fromIterable(pending).concatMapCompletable(statusNode -> execute(projectName, form, postedData, statusNode));
		}).subscribe(() -> {
		}, e -> log.error(String.format("Error while performing post processing for form data %s", formDataNode.getUuid()), e));
	}

	/**
	 * Retry failed and resume interrupted post processing actions for all projects.
	 * This is done only on the coordination master (if a coordinator is configured). The coordination master is only checked,
	 * when unfinished actions were found.
	 * @return completable
	 */
	public Completable runRecovery() {
		return Completable.defer(() -> {
			if (!recoveryRunning.compareAndSet(false, true)) {
				log.debug("Post processing job is still running, skipping");
				return Completable.complete();
			}

			return statusService.projectsWithPluginActive().toFlowable(BackpressureStrategy.BUFFER)
					.flatMapCompletable(project -> recover(project.getName()).onErrorComplete(e -> {
						log.error(String.format("Error while performing post processing job for project %s", project.getName()), e);
						return true;
					}), false, 1)
					.doFinally(() -> recoveryRunning.set(false));
		});
	}

	/**
	 * Retry failed and resume interrupted post processing actions for the given project
	 * @param projectName project name
	 * @return completable
	 */
	protected Completable recover(String projectName) {
		Map<String, Object> context = new HashMap<>();
		context.put("schemaName", SchemaInitializer.POSTPROCESSING_SCHEMA_NAME);
		String query;
		try {
			query = new Handlebars().compileInline(findPendingGraphQl).apply(Context.newContext(context));
		} catch (IOException e) {
			return Completable.error(e);
		}

		GraphQLRequest request = new GraphQLRequest().setQuery(query).setVariables(new JsonObject().put("perPage", RECOVERY_BATCH_SIZE));

		return adminClient.graphql(projectName, request).toSingle().flatMapCompletable(response -> {
			if (CollectionUtils.isNotEmpty(response.getErrors())) {
				for (GraphQLError error : response.getErrors()) {
					log.warn("Error in graphql response {}", error.toJson());
				}
			}

			List<Map<String, Object>> elements = Optional
					.ofNullable(FormUtil.<List<Map<String, Object>>>getFromResponse(response, "$.data.schema.nodes.elements[*]"))
					.orElse(Collections.emptyList());

			if (elements.isEmpty()) {
				return Completable.complete();
			}

			// only check the coordination master when there is something to do
			// (checking it without a configured coordinator logs an error in Mesh)
			return purgeService.shouldSkipPurge().flatMapCompletable(skip -> {
				if (skip) {
					log.debug("Skip post processing job, because currently not the master");
					return Completable.complete();
				}
				return resumeAll(projectName, elements);
			});
		});
	}

	/**
	 * Resume the post processing actions for the status nodes found by the query
	 * @param projectName project name
	 * @param elements status nodes found by the query
	 * @return completable
	 */
	private Completable resumeAll(String projectName, List<Map<String, Object>> elements) {
		return Flowable.fromIterable(elements).concatMapCompletable(element -> {
			String statusNodeUuid = Objects.toString(element.get("uuid"), null);
			String formDataUuid = getPath(element, "dataNode", "uuid");
			String formUuid = getPath(element, "dataNode", "dataFolder", "form", "uuid");
			if (statusNodeUuid == null || formDataUuid == null || formUuid == null) {
				log.warn("Could not determine form data and form for post processing status node {}, skipping", statusNodeUuid);
				return Completable.complete();
			}
			return recover(projectName, formUuid, formDataUuid, statusNodeUuid).onErrorComplete(e -> {
				log.error(String.format("Error while resuming post processing %s", statusNodeUuid), e);
				return true;
			});
		});
	}

	/**
	 * Resume the post processing action for the given status node, if it is due.
	 * @param projectName project name
	 * @param formUuid form uuid
	 * @param formDataUuid form data uuid
	 * @param statusNodeUuid status node uuid
	 * @return completable
	 */
	protected Completable recover(String projectName, String formUuid, String formDataUuid, String statusNodeUuid) {
		return loadNode(projectName, statusNodeUuid).flatMapCompletable(statusNode -> {
			if (!isDue(statusNode)) {
				return Completable.complete();
			}
			String language = getString(statusNode, FIELD_LANGUAGE);

			Single<Form> formSingle = statusService.getFormtypeConfigs(projectName, adminClient)
					.flatMap(formtypeConfigs -> formService.loadFormAsAdmin(projectName, formUuid)
							.flatMap(formNode -> formService.toForm(formtypeConfigs, formNode, language)));

			return Single.zip(formSingle, loadNode(projectName, formDataUuid), (form, formDataNode) -> {
				log.info("Resuming post processing action {} for form data {}", getString(statusNode, FIELD_KEY), formDataUuid);
				return execute(projectName, form, getPostedData(form, formDataNode, language), statusNode);
			}).flatMapCompletable(c -> c);
		});
	}

	/**
	 * Perform the post processing action of the status node and update the status
	 * @param projectName project name
	 * @param form form
	 * @param postedData posted data
	 * @param statusNode status node
	 * @return completable
	 */
	protected Completable execute(String projectName, Form form, PostedData postedData, NodeResponse statusNode) {
		String key = getString(statusNode, FIELD_KEY);
		String language = getString(statusNode, FIELD_LANGUAGE);
		int attempts = getAttempts(statusNode);
		PostProcessingOptions options = getOptions(form);
		Optional<PostProcessingAction> optAction = registry.get(key);

		if (optAction.isEmpty()) {
			log.error("Post processing action {} is not available, marking it as failed", key);
			return updateStatus(projectName, statusNode, PostProcessingStatus.FAILED, attempts, "Post processing action not available").ignoreElement();
		}

		PostProcessingAction action = optAction.get();

		// claim the action by setting it to RUNNING. If this fails (e.g. due to a conflict), the action is not performed
		return updateStatus(projectName, statusNode, PostProcessingStatus.RUNNING, attempts, null)
			.toMaybe()
			.onErrorResumeNext(e -> {
				log.warn(String.format("Could not set post processing action %s (%s) to running, skipping it", key, statusNode.getUuid()), e);
				return Maybe.empty();
			})
			.flatMapCompletable(runningNode -> Completable.defer(() -> action.process(options.getProperties(key), form, postedData, language))
				.toSingleDefault(Optional.<Throwable>empty())
				.onErrorReturn(Optional::of)
				.flatMapCompletable(optError -> {
					int performedAttempts = attempts + 1;
					if (optError.isEmpty()) {
						log.debug("Post processing action {} ({}) done", key, statusNode.getUuid());
						return updateStatus(projectName, runningNode, PostProcessingStatus.DONE, performedAttempts, null).ignoreElement();
					}
					Throwable error = optError.get();
					PostProcessingStatus newStatus = performedAttempts < options.getMaxAttempts() ? PostProcessingStatus.PENDING : PostProcessingStatus.FAILED;
					log.error(String.format("Post processing action %s (%s) failed in attempt %d, new status is %s", key, statusNode.getUuid(), performedAttempts, newStatus), error);
					return updateStatus(projectName, runningNode, newStatus, performedAttempts, Objects.toString(error.getMessage(), error.getClass().getName())).ignoreElement();
				}));
	}

	/**
	 * Check whether the post processing action of the status node is due for being performed by the periodic job
	 * @param statusNode status node
	 * @return true iff the action is due
	 */
	protected boolean isDue(NodeResponse statusNode) {
		PostProcessingStatus status = getStatus(statusNode);
		Instant lastUpdate = getLastUpdate(statusNode);
		Instant now = Instant.now();

		if (status == PostProcessingStatus.PENDING) {
			Duration retryInterval = Duration.ofSeconds(configuration.getPostProcessingOptions().getRetryInterval());
			return lastUpdate == null || !lastUpdate.plus(retryInterval).isAfter(now);
		} else if (status == PostProcessingStatus.RUNNING) {
			return lastUpdate == null || !lastUpdate.plus(RUNNING_TIMEOUT).isAfter(now);
		} else {
			return false;
		}
	}

	/**
	 * Load the status nodes, which are children of the given form data node
	 * @param projectName project name
	 * @param formDataUuid form data uuid
	 * @return single list of status nodes
	 */
	public Single<List<NodeResponse>> loadStatusNodes(String projectName, String formDataUuid) {
		return adminClient.findNodeChildren(projectName, formDataUuid, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE),
				new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages(NODE_LANGUAGE)).toSingle()
			.map(children -> children.getData().stream()
					.filter(node -> node.getSchema() != null && SchemaInitializer.POSTPROCESSING_SCHEMA_NAME.equals(node.getSchema().getName()))
					.collect(Collectors.toList()));
	}

	/**
	 * Create the status node for the action with given key
	 * @param projectName project name
	 * @param formDataUuid form data uuid
	 * @param key action key
	 * @param language language of the posted data
	 * @return completable
	 */
	protected Completable createStatusNode(String projectName, String formDataUuid, String key, String language) {
		NodeCreateRequest request = new NodeCreateRequest()
				.setSchemaName(SchemaInitializer.POSTPROCESSING_SCHEMA_NAME)
				.setParentNodeUuid(formDataUuid)
				.setLanguage(NODE_LANGUAGE);
		FieldMap fields = request.getFields();
		fields.put(FIELD_KEY, new StringFieldImpl().setString(key));
		fields.put(FIELD_LANGUAGE, new StringFieldImpl().setString(language));
		setStatusFields(fields, PostProcessingStatus.PENDING, 0, null);

		return adminClient.createNode(projectName, request).toCompletable()
				.doOnComplete(() -> log.debug("Created post processing status for action {} of form data {}", key, formDataUuid));
	}

	/**
	 * Update the status of the status node
	 * @param projectName project name
	 * @param statusNode status node (the version of this node will be used for the update)
	 * @param status new status
	 * @param attempts number of performed attempts
	 * @param error optional error message
	 * @return single updated node
	 */
	protected Single<NodeResponse> updateStatus(String projectName, NodeResponse statusNode, PostProcessingStatus status, int attempts, String error) {
		NodeUpdateRequest request = new NodeUpdateRequest()
				.setLanguage(NODE_LANGUAGE)
				.setVersion(statusNode.getVersion());
		setStatusFields(request.getFields(), status, attempts, error);
		return adminClient.updateNode(projectName, statusNode.getUuid(), request).toSingle();
	}

	/**
	 * Get the posted data from the form data node. The data is transformed into the same format as data posted by the form generator.
	 * @param form form
	 * @param formDataNode form data node
	 * @param language language
	 * @return posted data
	 */
	protected PostedData getPostedData(Form form, NodeResponse formDataNode, String language) {
		FormData formData = FormService.buildFormDataFromNode(new PostedData(form), form, null, Collections.emptyMap(), language, formDataNode);
		FormData converted = objectMapper.convertValue(formData, FormData.class);
		return converted.getProperties() != null ? new PostedData(form, converted.getProperties()) : new PostedData(form);
	}

	private Single<NodeResponse> loadNode(String projectName, String uuid) {
		return adminClient.findNodeByUuid(projectName, uuid, new VersioningParametersImpl().draft(), new NodeParametersImpl().setLanguages(NODE_LANGUAGE)).toSingle();
	}

	private PostProcessingOptions getOptions(Form form) {
		return Optional.ofNullable(form.getConfiguration()).orElse(configuration).getPostProcessingOptions();
	}

	private static void setStatusFields(FieldMap fields, PostProcessingStatus status, int attempts, String error) {
		fields.put(FIELD_STATUS, new StringFieldImpl().setString(status.name()));
		fields.put(FIELD_ATTEMPTS, new NumberFieldImpl().setNumber(attempts));
		fields.put(FIELD_ERROR, new StringFieldImpl().setString(StringUtils.defaultString(error)));
		fields.put(FIELD_LAST_UPDATE, new DateFieldImpl().setDate(Instant.now().toString()));
	}

	private static String getString(NodeResponse node, String fieldName) {
		// node listings may return nodes without fields (observed while the node was concurrently updated)
		if (node.getFields() == null) {
			return null;
		}
		StringField field = node.getFields().getStringField(fieldName);
		return field != null ? field.getString() : null;
	}

	private static PostProcessingStatus getStatus(NodeResponse node) {
		String status = getString(node, FIELD_STATUS);
		try {
			return status != null ? PostProcessingStatus.valueOf(status) : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static int getAttempts(NodeResponse node) {
		if (node.getFields() == null) {
			return 0;
		}
		NumberField field = node.getFields().getNumberField(FIELD_ATTEMPTS);
		return field != null && field.getNumber() != null ? field.getNumber().intValue() : 0;
	}

	private static Instant getLastUpdate(NodeResponse node) {
		if (node.getFields() == null) {
			return null;
		}
		DateField field = node.getFields().getDateField(FIELD_LAST_UPDATE);
		if (field == null || StringUtils.isBlank(field.getDate())) {
			return null;
		}
		try {
			return Instant.parse(field.getDate());
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String getPath(Map<String, Object> element, String... path) {
		Object current = element;
		for (String segment : path) {
			if (current instanceof Map<?, ?> map) {
				current = map.get(segment);
			} else {
				return null;
			}
		}
		return current != null ? current.toString() : null;
	}
}
