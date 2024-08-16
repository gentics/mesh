package com.gentics.mesh.plugin.common.service;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.common.model.SchemaCreateOrUpdateResponse;
import com.gentics.mesh.plugin.common.util.MiscUtil;
import com.gentics.mesh.plugin.common.util.ReadInputStream;
import com.gentics.mesh.plugin.common.util.RxUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.google.common.collect.Sets;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

/**
 * This utility service provides methods commonly used by Mesh plugins,
 * especially during initialization.
 *
 * <p>
 *     The {@link #prepareProject(String, InitializationAction[]) prepareProject()} and
 *     {@link #prepareProjects(Collection, InitializationAction[]) prepareProjects()}
 *     methods can be used to make sure certain actions are performed to
 *     initialize projects for which the plugin should be active.
 * </p>
 *
 * <p>
 *     Currently the available initialization actions are
 *     <ul>
 *         <li>
 *             Make sure a certain folder exists with
 *             {@link #ensureFolders(String, String)}, and
 *         </li>
 *         <li>
 *             Make sure certain schemas exist, are up to date and assigned
 *             to the project with {@link #ensureSchemas(String...)}
 *         </li>
 *     </ul>
 * </p>
 */
public class MeshPluginService {

	// TODO This should be fixed in Mesh. The i18n key of the error message should always be sent as well.
	private static final Set<String> NO_MIGRATION_MSG = Sets.newHashSet(
		"schema_update_no_difference_detected",
		"Migration was not invoked. No changes were detected.",
		"Migration wurde nicht gestartet. Es wurden keine Änderungen erkannt."
	);

	private static final Logger log = LoggerFactory.getLogger(MeshPluginService.class);

	private final Vertx vertx;
	private final MeshRestClient adminClient;
	private final String schemaBaseDir;
	private final String storageDirPath;
	private final Set<MessageConsumer<?>> registeredConsumers = new HashSet<>();

	/**
	 * Convenience constructor using the {@link #schemaBaseDir}
	 * <code>/schemas/</code>.
	 *
	 * @param vertx The Vert.x instance to use
	 * @param adminClient The admin client to use
	 */
	public MeshPluginService(Vertx vertx, MeshRestClient adminClient, File storageDir) {
		this(vertx, adminClient, storageDir, "/schemas/");
	}

	public MeshPluginService(Vertx vertx, MeshRestClient adminClient, File storageDir, String schemaBaseDir) {
		this.vertx = vertx;
		this.adminClient = adminClient;
		this.schemaBaseDir = schemaBaseDir;
		this.storageDirPath = storageDir.getPath();
	}

	/**
	 * Make sure the given <code>actions</code> are performed for the specified
	 * <code>projects</code>.
	 *
	 * <p>
	 *     This method is meant to be called during
	 *     {@link MeshPlugin#initialize() plugin initialization}.
	 * </p>
	 *
	 * <p>
	 *     A {@link MessageConsumer} is created for the
	 *     {@link MeshEvent#PROJECT_CREATED} address, so that the actions will
	 *     also be executed on newly created projects if necessary.
	 * </p>
	 *
	 * <p>
	 *     <strong>Important:</strong> when projects are prepared with this
	 *     method, make sure to call {@link #unregisterEvents()} when the plugin
	 *     {@link MeshPlugin#shutdown() shuts down}.
	 * </p>
	 *
	 * @see #unregisterEvents()
	 * @param projects The projects the actions should be executed for
	 * @param actions The actions to execute for each project
	 * @return A <code>Completable</code> that completes when all already
	 * 		available projects are initialized, and the necessary events are
	 * 		registered
	 */
	public final Completable prepareProjects(Collection<String> projects, InitializationAction ...actions) {
		return prepareProjects(projects::contains, actions);
	}

	/**
	 * Convenience method for {@link #prepareProjects(Collection, InitializationAction[])}
	 * which will perform the actions for <em>all</em> projects.
	 *
	 * @param actions The actions to perform
	 * @return A <code>Completable</code> that completes when all already
	 * 		available projects are initialized, and the necessary events are
	 * 		registered
	 */
	public Completable prepareProjects(InitializationAction ...actions) {
		return prepareProjects(ignored -> true, actions);
	}

	/**
	 * Convenience method which calls
	 * {@link #prepareProjects(Collection, InitializationAction[]) prepareProjects} just for
	 * the one project.
	 *
	 * <p>
	 *     <strong>Important:</strong> when a project is prepared with this
	 *     method, make sure to call {@link #unregisterEvents()} when the plugin
	 *     {@link MeshPlugin#shutdown() shuts down}.
	 * </p>
	 *
	 * @see #prepareProjects(Collection, InitializationAction[])
	 * @see #unregisterEvents()
	 * @param project The project to be prepared
	 * @param actions The actions to perform once the project is available
	 * @return A completable representing the specified <code>actions</code>
	 */
	public final Completable prepareProject(String project, InitializationAction ...actions) {
		return prepareProjects(Collections.singleton(project), actions);
	}

	/**
	 * Unregister all event listeners created by the methods of this class.
	 *
	 * @return A <code>Completable</code> which will complete once all event
	 * 		listeners are unregistered
	 */
	public Completable unregisterEvents() {
		return Observable.fromIterable(registeredConsumers)
			.flatMapCompletable(MessageConsumer::rxUnregister)
			.doOnTerminate(() -> {
				if (log.isInfoEnabled()) {
					log.info(
						"Unregistered {} event{}",
						registeredConsumers.size(),
						registeredConsumers.size() == 1 ? "" : "s");
				}

				registeredConsumers.clear();
			})
			.doOnError(e -> log.error("Error while unregistering events", e));
	}

	/**
	 * Returns a function that will make sure the specified schemas exist and
	 * are assigned to the project.
	 *
	 * @param schemaNames Names of the schemas to check for existence and
	 * 		assignment
	 * @return A function that will make sure the schemas exist and are assigned
	 * 		to the project
	 */
	public InitializationAction ensureSchemas(String prefix, List<String> schemaNames) {
		Observable<Message<JsonObject>> events = registerEvent(MeshEvent.SCHEMA_MIGRATION_FINISHED, false);

		return project -> adminClient.findSchemas().toSingle()
			.doOnSubscribe(ignored -> log.info("Checking schemas for project {}", project))
			.flatMapObservable(existing -> Observable.fromIterable(schemaNames)
				.flatMapSingle(schema -> ensureSchemaExists(project, existing, prefix, schema)))
			.filter(SchemaCreateOrUpdateResponse::isUpdated)
			.map(s -> s.getSchema().getUuid())
			.toList()
			.flatMapCompletable(migratedSchemas -> {
				if (log.isInfoEnabled()) {
					int count = migratedSchemas.size();

					if (count < 1) {
						log.info("No schema migrations necessary");
					} else {
						log.info(
							"Waiting for {} schema migration{}: [{}]",
							count,
							count == 1 ? "" : "s",
							String.join(", ", migratedSchemas));
					}
				}

				return events
					.filter(schemaMigrationFinishedFilter(migratedSchemas))
					.take(migratedSchemas.size())
					.ignoreElements();
			});
	}

	/**
	 * Returns a function that will make sure the specified schemas exist and
	 * are assigned to the project.
	 *
	 * @param schemaNames Names of the schemas to check for existence and
	 * 		assignment
	 * @return A function that will make sure the schemas exist and are assigned
	 * 		to the project
	 */
	public InitializationAction ensureSchemas(String ...schemaNames) {
		return ensureSchemas("", Arrays.asList(schemaNames));
	}

	/**
	 * Makes sure that there is a node with the provided path.
	 *
	 * @param folderSchemaName The name of the folder schema to use for
	 * 		creating missing folders
	 * @param path The path to check
	 * @return A function that will take the project name, and return a
	 * 		<code>Completable</code> which creates the folder for the
	 * 		project
	 */
	public InitializationAction ensureFolders(String folderSchemaName, String path) {
		return project -> ensureFolders(project, folderSchemaName, path, false).ignoreElement();
	}

	/**
	 * Makes sure that there is a node with the provided path.
	 *
	 * @param project The project name
	 * @param folderSchemaName The name of the folder schema to use for
	 * 		creating missing folders
	 * @param path The path to check
	 * @param publish If true, the created folders are also published
	 * @return A <code>Single</code> with the node response for the last folder
	 * 		in the path
	 */
	public Single<NodeResponse> ensureFolders(String project, String folderSchemaName, String path, boolean publish) {
		// TODO create and assign minimal folder schema if needed.
		return adminClient.findSchemas().toSingle()
			.doOnSubscribe(ignored -> log.info("Checking {} \"{}\" for \"{}\"", folderSchemaName, path, project))
			.flatMapObservable(r -> Observable.fromIterable(r.getData()))
			.filter(s -> s.getName().equals(folderSchemaName))
			.singleOrError()
			.doOnError(e -> log.error("Could not find folder schema \"" + folderSchemaName + "\"", e))
			.zipWith(prefixPath(project, path), Pair::of)
			.flatMap(pair -> ensureFoldersRec(project, pair.getLeft(), pair.getRight(), publish));
	}

	/**
	 * Load a file from the given path in the storage directory.
	 *
	 * <p>
	 *     Note that the {@link #storageDirPath} is relative to the current working directory.
	 * </p>
	 *
	 * @param path The path (starting from the storage directory) to load the file from
	 * @return A single with an <code>InputStream</code> containing the loaded data
	 */
	public Single<InputStream> loadFromStorage(String path) {
		return vertx.fileSystem().rxOpen(storageDirPath + path, new OpenOptions().setRead(true)).map(ReadInputStream::new);
	}

	/**
	 * Perform the specified actions for all projects matching <code>projectsFilter</code>.
	 *
	 * @param projectsFilter Filter for projects to initialize
	 * @param actions The actions to perform for initialization
	 * @return A <code>Completable</code> that completes when all already
	 * 		available projects are initialized, and the necessary events are
	 * 		registered
	 */
	private Completable prepareProjects(Predicate<String> projectsFilter, InitializationAction ...actions) {
		return adminClient.findProjects().toObservable()
			.flatMap(rs -> Observable.fromIterable(rs.getData()))
			.map(ProjectResponse::getName)
			.filter(projectsFilter)
			.flatMap(project -> initProject(project, actions).toObservable(), 1)
			.ignoreElements()
			.doOnComplete(() -> registerEvent(MeshEvent.PROJECT_CREATED, true)
				.map(evt -> evt.body().getString("name"))
				.filter(projectsFilter)
				.flatMap(project -> initProject(project, actions).toObservable(), 1)
				.ignoreElements()
				.subscribe());
	}

	/**
	 * Create a consumer for the specified event and add it to the set of
	 * {@link #registeredConsumers registered consumers}.
	 *
	 * @param event The event to listen for
	 * @param local Whether to use a local consumer to handle only local events.
	 * @return The <code>Observable</code> created from the
	 * 		{@link MessageConsumer}.
	 */
	private Observable<Message<JsonObject>> registerEvent(MeshEvent event, boolean local) {
		EventBus eb = vertx.eventBus();
		MessageConsumer<JsonObject> consumer = local ? eb.localConsumer(event.address) : eb.consumer(event.address);

		registeredConsumers.add(consumer);

		return consumer.toObservable()
			.doOnDispose(() -> {
				registeredConsumers.remove(consumer);
				consumer.unregister();
			});
	}

	/**
	 * Execute the given actions on the project.
	 *
	 * @param project The project to initialize
	 * @param actions The actions to perform for initialization
	 * @return A <code>Completable</code> that completes when all the
	 * 		<code>actions</code> have been performed
	 */
	private Completable initProject(String project, InitializationAction ...actions) {
		return Flowable.fromArray(actions)
			.doOnSubscribe(ignored -> {
				if (log.isInfoEnabled()) {
					log.info(
						"Initializing project \"{}\" ({} action{})",
						project,
						actions.length,
						actions.length == 1 ? "" : "s");
				}
			})
			.concatMapCompletable(action -> action.apply(project), 1)
			.doOnError(e -> log.error("Project initialization failed", e));
	}

	/**
	 * Creates a filter for {@link MeshEvent#SCHEMA_MIGRATION_FINISHED} events
	 * that match the given schema names.
	 *
	 * @param migratedSchemas The desired schema names
	 * @return A filter for <code>SCHEMA_MIGRATION_FINISHED</code> events that
	 * 		match the given schema names
	 */
	private Predicate<Message<JsonObject>> schemaMigrationFinishedFilter(Collection<String> migratedSchemas) {
		return msg -> {
			String address = msg.address();
			String uuid = msg.body().mapTo(SchemaMigrationMeshEventModel.class).getToVersion().getUuid();
			boolean ret = MeshEvent.SCHEMA_MIGRATION_FINISHED.address.equals(address) && migratedSchemas.contains(uuid);

			if (log.isDebugEnabled()) {
				log.debug("Received event {}: {}; filter: {}", address, uuid, ret);
			}

			return ret;
		};
	}

	/**
	 * Make sure the schema with the specified name exists and is assigned to
	 * the given project.
	 *
	 * <p>
	 *     When the schema already exists, it will be updated, otherwise it will
	 *     be created.
	 * </p>
	 *
	 * @param project The project to check
	 * @param existing The existing schemas
	 * @param schemaName The schema name to check
	 * @return An <code>Single</code> with the updated or created schema
	 * 		response
	 */
	private Single<SchemaCreateOrUpdateResponse> ensureSchemaExists(String project, SchemaListResponse existing, String prefix, String schemaName) {
		List<SchemaResponse> schemas = existing.getData();

		if (schemas == null) {
			schemas = Collections.emptyList();
		}

		return schemas.stream()
			.filter(schema -> schema.getName().equals(prefix + schemaName))
			.findAny()
			.map(response -> updateSchema(schemaName, response))
			.orElseGet(() -> createSchema(prefix, schemaName))
			.flatMap(schema -> {
				if (log.isInfoEnabled()) {
					log.info("Assigning schema \"{}\" to \"{}\"", schema.getSchema().getName(), project);
				}

				return adminClient.assignSchemaToProject(project, schema.getSchema().getUuid()).toSingle()
					.map(ignored -> schema);
			});
	}

	/**
	 * Make sure the specified path exists in the project.
	 *
	 * @param project The project to check
	 * @param folderSchema The schema to use for creating missing folders
	 * @param path The path to check
	 * @param publish If true, the created folders are also published
	 * @return A <code>Single</code> with the node response for the last folder
	 * 		in the path
	 */
	private Single<NodeResponse> ensureFoldersRec(String project, SchemaResponse folderSchema, String path, boolean publish) {
		return RxUtil.webrootMaybe(adminClient, project, path)
			.to(RxUtil.defaultSingle(() -> {
				String[] split = splitLastSegment(path);

				return ensureFoldersRec(project, folderSchema, split[0], publish)
					.flatMap(parent -> createFolder(project, folderSchema, parent.getUuid(), split[1]))
					.flatMap(node -> publish
						? adminClient.publishNode(project, node.getUuid()).toCompletable().andThen(Single.just(node))
						: Single.just(node));
			}));
	}

	/**
	 * Update the specified schema with the definition loaded from the
	 * resources.
	 *
	 * <p>
	 *     Note that the schema will not be loaded again after the update, the
	 *     {@link SchemaCreateOrUpdateResponse} in the resulting Observable will
	 *     be the one passed to the method. So only the schema name and the
	 *     {@link SchemaCreateOrUpdateResponse#isUpdated() isUpdated} property
	 *     of the response should be used further on.
	 * </p>
	 *
	 * @param schema The schema to update
	 * @return An <code>Single</code> with the schema (before the update)
	 */
	private Single<SchemaCreateOrUpdateResponse> updateSchema(String schemaName, SchemaResponse schema) {
		SchemaUpdateRequest request = JsonUtil.readValue(
			MiscUtil.loadResource(schemaBaseDir + schemaName + ".json"),
			SchemaUpdateRequest.class);

		if (request.getContainer() == null) {
			// This is the default in a SchemaCreateRequest and the same JSON
			// files are used for creating and updating schemas.
			request.setContainer(false);
		}
		// Make sure to not change the name
		request.setName(schema.getName());

		return adminClient.updateSchema(schema.getUuid(), request).toSingle()
			.map(response -> {
				String msg = response.getMessage();
				boolean updated = !NO_MIGRATION_MSG.contains(msg);

				if (log.isInfoEnabled()) {
					if (updated) {
						log.info("Updated schema \"{}\": {}", schema.getName(), msg);
					} else {
						log.info("Schema \"{}\" is still valid", schema.getName());
					}
				}

				return new SchemaCreateOrUpdateResponse(schema, updated);
			});
	}

	/**
	 * Create the schema with the given name.
	 *
	 * <p>
	 *     The schema definition is loaded from the resources using the path
	 *     <em>/schemaBaseDir/schemaName.json</em>.
	 * </p>
	 *
	 * @param schemaName The schema to create
	 * @return An <code>Single</code> with the created schema
	 */
	private Single<SchemaCreateOrUpdateResponse> createSchema(String prefix, String schemaName) {
		SchemaCreateRequest request = JsonUtil.readValue(
			MiscUtil.loadResource(schemaBaseDir + schemaName + ".json"),
			SchemaCreateRequest.class);

		request.setName(prefix + schemaName);

		return adminClient.createSchema(request).toSingle()
			.doOnError(e -> log.error("FATAL: Could not create schema \"{}\": {}", schemaName, e.getMessage()))
			.doOnSuccess(r -> log.info("Created schema \"{}\" with UUID {}", r.getName(), r.getUuid()))
			.map(schema -> new SchemaCreateOrUpdateResponse(schema, false));
	}

	/**
	 * Creates the specified folder.
	 *
	 * <p>
	 *     The <code>folderName</code> is used as the segment and display field
	 *     for the created folder.
	 * </p>
	 *
	 * @param projectName The project to create the folder in
	 * @param folderSchema The schema to use for creating the folder
	 * @param parentUuid The parent UUID
	 * @param folderName The name of the folder to create
	 * @return A <code>Single</code> with the created folders <code>NodeResponse</code>
	 */
	private Single<NodeResponse> createFolder(String projectName, SchemaResponse folderSchema, String parentUuid, String folderName) {
		NodeCreateRequest createRequest = new NodeCreateRequest()
			.setLanguage("en")
			.setParentNodeUuid(parentUuid)
			.setSchemaName(folderSchema.getName());
		FieldMap fields = createRequest.getFields();

		if (StringUtils.isBlank(folderSchema.getSegmentField())) {
			log.warn("The used folder schema does not define a segment field, this WILL cause errors later on");
		} else {
			fields.put(folderSchema.getSegmentField(), new StringFieldImpl().setString(folderName));
		}

		if (!StringUtils.isBlank(folderSchema.getDisplayField())) {
			fields.put(folderSchema.getDisplayField(), new StringFieldImpl().setString(folderName));
		}

		if (log.isTraceEnabled()) {
			log.trace("Creating {} \"{}\" in parent {}", folderSchema.getName(), folderName, parentUuid);
		}
		return adminClient.createNode(projectName, createRequest).toSingle()
			.doOnError(e -> log.error("Could not create folder " + folderName, e))
			.doOnSuccess(folder -> log.info("Created folder {} [{}]", folderName, folder.getUuid()));
	}

	/**
	 * Removes all trailing slashes and then splits the last segment of the
	 * provided path.
	 *
	 * @param path The path to split
	 * @return An array where the first entry contains the path without the
	 * 		last segment, and the second entry contains only the last segment
	 */
	private String[] splitLastSegment(String path) {
		// Remove trailing slashes
		if (path.length() > 1) {
			path = path.replaceAll("/*$", "");
		}

		int index = path.lastIndexOf('/');

		return new String[] {
			index == 0 ? "/" : path.substring(0, index),
			path.substring(index + 1)
		};
	}

	/**
	 * Make sure, that the path is prefixed with the pathPrefix of the project's default branch
	 * @param project project name
	 * @param path given path
	 * @return single of prefixed path (or the original path, if that already started with the path prefix)
	 */
	private Single<String> prefixPath(String project, String path) {
		return adminClient
				.findBranches(project, new GenericParametersImpl().setETag(false).setFields("latest", "pathPrefix"))
				.toSingle()
				.flatMap(branchList -> Observable.fromIterable(branchList.getData())
						.filter(branch -> Objects.equals(Boolean.TRUE, branch.getLatest()))
						.map(BranchResponse::getPathPrefix).firstOrError())
				.map(pathPrefix -> {
					if (StringUtils.isBlank(pathPrefix)) {
						return path;
					} else if (path.startsWith(pathPrefix)) {
						return path;
					} else {
						// prefix the path with the pathPrefix
						StringBuilder prefixed = new StringBuilder();
						// ensure that the prefixed path starts with a single /
						if (!pathPrefix.startsWith("/")) {
							prefixed.append("/");
						}
						prefixed.append(pathPrefix);
						// make sure, that there is a single / between the prefix and the path
						if (!pathPrefix.endsWith("/")) {
							prefixed.append("/");
						}
						// but only one /
						prefixed.append(path.startsWith("/") ? path.substring(1) : path);
						return prefixed.toString();
					}
				});
	}
}
