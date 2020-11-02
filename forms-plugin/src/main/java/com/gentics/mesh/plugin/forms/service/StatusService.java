package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.model.LanguagesModel;
import com.gentics.mesh.plugin.forms.model.response.StatusResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Service for handling activation status of the plugin for projects
 */
@Singleton
public class StatusService {
	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final SchemaInitializer schemaInitializer;

	private final SchemaService schemaService;

	private final String pluginRootNodeQuery;

	@Inject
	public StatusService(@AdminClient MeshRestClient adminClient, @UserClient @Nullable MeshRestClient userClient,
			SchemaInitializer schemaInitializer, SchemaService schemaService) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.schemaInitializer = schemaInitializer;
		this.schemaService = schemaService;

		try {
			pluginRootNodeQuery = IOUtils.toString(FormsPlugin.class.getResource("forms_plugin_node.graphql"), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the plugin
	 * @return completable
	 */
	public Completable initialize() {
		return projectsWithPluginRootNode().flatMapCompletable(project -> schemaInitializer.initialize(project.getName()));
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
			DocumentContext ctx = JsonPath.parse(response.toJson(), Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
			List<Boolean> published = ctx.read("$.data.schema.nodes.elements[*].languages[*].isPublished");

			return new StatusResponse().setActive(published.stream().filter(b -> b == Boolean.TRUE).findFirst().isPresent());
		});
	}

	/**
	 * Activate the plugin in the project
	 * @param projectName project name
	 * @return activation response
	 */
	public Single<StatusResponse> activate(String projectName) {
		return schemaInitializer.initialize(projectName).andThen(publish(projectName));
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
	 * @param projectName project name
	 * @return single languages
	 */
	public Single<LanguagesModel> getLanguages(String projectName) {
		return userClient.findNodeByUuid(projectName, schemaService.getRootNodeUuid(projectName),
				new GenericParametersImpl().setETag(false).setFields("fields")).toSingle().flatMap(response -> {
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
		return adminClient.findNodeByUuid(projectName, schemaService.getRootNodeUuid(projectName),
				new GenericParametersImpl().setETag(false).setFields("fields")).toSingle().flatMap(response -> {
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
		return userClient.updateNode(projectName, schemaService.getRootNodeUuid(projectName), update,
				new GenericParametersImpl().setETag(false).setFields("fields", "uuid")).toSingle().flatMap(response -> {
					return userClient.publishNode(projectName, response.getUuid()).toSingle().flatMap(r -> {
						FieldMap fields = response.getFields();
						return Single.just(new LanguagesModel().setLanguages(
								fields.hasField("languages") ? fields.getStringFieldList("languages").getItems()
										: null));
					});
				});
	}

	/**
	 * Return an observable emitting all project responses for projects that have the plugin root node
	 * @return observable
	 */
	private Observable<ProjectResponse> projectsWithPluginRootNode() {
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
}
