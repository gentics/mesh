package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.model.LanguagesModel;
import com.gentics.mesh.plugin.forms.model.response.StatusResponse;
import com.gentics.mesh.plugin.forms.util.FormUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;

/**
 * Service for handling activation status of the plugin for projects
 */
@Singleton
public class StatusService {

	private static final Logger log = LoggerFactory.getLogger(StatusService.class);

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final SchemaInitializer schemaInitializer;
	private final MeshPluginService meshPluginService;
	private final Lazy<PurgeService> lazyPurgeService;

	private final String pluginRootNodeQuery;
	private final Vertx rxVertx;

	@Inject
	public StatusService(
			@AdminClient MeshRestClient adminClient,
			@UserClient @Nullable MeshRestClient userClient,
			SchemaInitializer schemaInitializer,
			MeshPluginService meshPluginService,
			Lazy<PurgeService> lazyPurgeService,
			io.vertx.core.Vertx vertx,
			FormsConfig config) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.schemaInitializer = schemaInitializer;
		this.meshPluginService = meshPluginService;
		this.lazyPurgeService = lazyPurgeService;
		this.rxVertx = Vertx.newInstance(vertx);

		try {
			pluginRootNodeQuery = IOUtils.toString(FormsPlugin.class.getResource("forms_plugin_node.graphql"), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the plugin.
	 *
	 * <p>
	 *     Creates the storage directory if it does not already exist.
	 * </p>
	 *
	 * @param storageDirPath The path to the storage directory for the plugin.
	 * @return completable
	 */
	public Completable initialize(String storageDirPath) {
		return rxVertx.fileSystem().rxMkdirs(storageDirPath)
			.andThen(meshPluginService.ensureSchemasGlobal(StringUtils.EMPTY, SchemaInitializer.PLUGIN_SCHEMAS))
			.flatMapCompletable(schemas -> projectsWithPluginActive()
				.flatMapCompletable(project -> initializeProject(project.getName(), Optional.of(schemas))));
	}

	/**
	 * Check with the user client, whether the project contains the plugin root node, and whether it is published
	 * @param projectName project name
	 * @return single activation response
	 */
	public Single<StatusResponse> check(String projectName) {
		return check(userClient, projectName);
	}

	/**
	 * Check with the given client, whether the project contains the plugin root node, and whether it is published
	 * @param client mesh rest client to use
	 * @param projectName project name
	 * @return single activation response
	 */
	private Single<StatusResponse> check(MeshRestClient client, String projectName) {
		return client.graphqlQuery(projectName, pluginRootNodeQuery).toSingle().map(response -> {
			List<Boolean> published = FormUtil.getFromResponse(response, "$.data.schema.nodes.elements[*].languages[*].isPublished");

			return new StatusResponse().setActive(published.stream().filter(b -> b == Boolean.TRUE).findFirst().isPresent());
		});
	}

	/**
	 * Activate the plugin in the project
	 * @param projectName project name
	 * @return activation response
	 */
	@Deprecated
	public Single<StatusResponse> activate(String projectName) {
		return activate(projectName, Optional.empty());
	}

	/**
	 * Activate the plugin in the project
	 * @param projectName project name
	 * @return activation response
	 */
	public Single<StatusResponse> activate(String projectName, Optional<SchemaListResponse> maybeExistingSchemas) {
		return initializeProject(projectName, maybeExistingSchemas).andThen(publish(projectName));
	}

	/**
	 * Initialize the project.
	 *
	 * @param projectName
	 * @return
	 */
	protected Completable initializeProject(String projectName, Optional<SchemaListResponse> maybeExistingSchemas) {
		return schemaInitializer.initialize(projectName, maybeExistingSchemas);
			}

	/**
	 * Publish the form generator root node
	 * @param projectName project name
	 * @return activation response
	 */
	protected Single<StatusResponse> publish(String projectName) {
		// find all nodes of schema "forms_plugin" in the node
		return userClient.graphqlQuery(projectName, pluginRootNodeQuery).toSingle().flatMap(response -> {
			// parse node uuid's
			DocumentContext ctx = JsonPath.parse(response.toJson(),
					Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
			List<String> elementUuids = ctx.read("$.data.schema.nodes.elements[*].uuid");

			// publish all nodes (not recursively) and return status
			return Observable.fromIterable(elementUuids)
					.flatMapCompletable(uuid -> userClient
							.publishNode(projectName, uuid, new PublishParametersImpl().setRecursive(false))
							.toCompletable())
					.andThen(check(projectName));
		});
	}

	/**
	 * Deactivate the plugin in the project (if active)
	 * @param projectName project name
	 * @return activation response
	 */
	public Single<StatusResponse> deactivate(String projectName) {
		// find all nodes of schema "forms_plugin" in the node
		return userClient.graphqlQuery(projectName, pluginRootNodeQuery).toSingle().flatMap(response -> {
			// parse node uuid's
			DocumentContext ctx = JsonPath.parse(response.toJson(),
					Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
			List<String> elementUuids = ctx.read("$.data.schema.nodes.elements[*].uuid");

			// take all nodes offline (recursively) and return status
			return Observable.fromIterable(elementUuids)
					.flatMapCompletable(uuid -> userClient
							.takeNodeOffline(projectName, uuid, new PublishParametersImpl().setRecursive(true))
							.toCompletable())
					.andThen(check(projectName));
		});
	}

	/**
	 * Get the languages for the given project
	 *
	 * <p>
	 *     Convenience method for {@link #getLanguages(String, MeshRestClient)}
	 *     which uses the {@code userClient}.
	 * </p>
	 *
	 * @param projectName project name
	 * @return single languages
	 */
	public Single<LanguagesModel> getLanguages(String projectName) {
		return getLanguages(projectName, userClient);
	}

	/**
	 * Get the languages for the given project, using the given Mesh REST client.
	 *
	 * @param projectName The project name
	 * @param client The Mesh REST client to use
	 * @return A {@code Single} containing the available form languages for the project
	 */
	public Single<LanguagesModel> getLanguages(String projectName, MeshRestClient client) {
		return getRootNode(projectName, client).flatMap(response -> {
			FieldMap fields = response.getFields();
			return Single.just(new LanguagesModel().setLanguages(
				fields.hasField("languages") ? fields.getStringFieldList("languages").getItems() : null));
		});
	}

	/**
	 * Get the languages for the given project
	 * @param projectName project name
	 * @return single languages
	 */
	public Single<String[]> languageFallback(String projectName, String...langs) {
		return getRootNode(projectName, adminClient).flatMap(response -> {
					FieldMap fields = response.getFields();
					List<String> langList = new ArrayList<>();
					langList.addAll(Arrays.asList(langs));
					if (fields.hasField("languages")) {
						for (String fallbackLang : fields.getStringFieldList("languages").getItems()) {
							if (!langList.contains(fallbackLang)) {
								langList.add(fallbackLang);
							}
						}
					}

					return Single.just(langList.toArray(new String[langList.size()]));
				});
	}

	/**
	 * Set the languages for the given project
	 * @param projectName project name
	 * @param languages language code list
	 * @return single languages
	 */
	public Single<LanguagesModel> setLanguages(String projectName, List<String> languages) {
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setVersion("draft");
		update.setLanguage("en");
		update.getFields().put("languages", new StringFieldListImpl().setItems(languages));
		return getRootNode(projectName, userClient).flatMap(rootNode -> {
			return userClient.updateNode(projectName, rootNode.getUuid(), update,
					new GenericParametersImpl().setETag(false).setFields("fields", "uuid")).toSingle().flatMap(response -> {
						return userClient.publishNode(projectName, response.getUuid()).toSingle().flatMap(r -> {
							FieldMap fields = response.getFields();
							return Single.just(new LanguagesModel().setLanguages(
									fields.hasField("languages") ? fields.getStringFieldList("languages").getItems()
											: null));
						});
					});
		});
	}

	/**
	 * Return an observable emitting all project responses for projects that have the plugin activated
	 * @return observable
	 */
	public Observable<ProjectResponse> projectsWithPluginActive() {
		return adminClient.findProjects().toSingle()
			.flatMapObservable(response -> Observable.fromIterable(response.getData())).flatMap(project -> {
				return check(adminClient, project.getName()).flatMapObservable(status -> {
					if (status.isActive()) {
						return Observable.just(project);
					} else {
						return Observable.empty();
					}
				});
			});
	}

	/**
	 * Load the FormsPlugin Root Node for the project.
	 *
	 * @param projectName project name
	 * @param client client to use
	 * @return single node response
	 */
	public Single<NodeResponse> getRootNode(String projectName, MeshRestClient client) {
		return getPathPrefixOfDefaultBranch(projectName)
			.flatMap(pathPrefix -> {
				String path = pathPrefix + StringUtils.prependIfMissing(SchemaInitializer.PATH_FORMS_PLUGIN_ROOT, "/");
				GenericParameters params = new GenericParametersImpl().setETag(false).setFields("fields", "uuid");

				return client.webroot(projectName, path, params)
					.toSingle()
					.doAfterSuccess(MeshWebrootResponse::close)
					.map(MeshWebrootResponse::getNodeResponse);
			});
	}

	/**
	 * Get the path prefix of the project's default branch. If no prefix is set, it defaults to empty string
	 * @param projectName project name
	 * @return single path prefix
	 */
	public Single<String> getPathPrefixOfDefaultBranch(String projectName) {
		return adminClient.findBranches(projectName).toSingle()
				.flatMapObservable(branchesListResponse -> Observable.fromIterable(branchesListResponse.getData()))
				.filter(branch -> branch.getLatest() != null && branch.getLatest())
				.map(branch -> branch.getPathPrefix()).first("");
	}
	}
