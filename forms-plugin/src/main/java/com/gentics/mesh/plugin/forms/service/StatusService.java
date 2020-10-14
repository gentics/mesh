package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.parameter.client.PublishParametersImpl;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.model.response.StatusResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Service for handling activation status of the plugin for projects
 */
@Singleton
public class StatusService {
	private final MeshRestClient userClient;

	private final SchemaInitializer schemaInitializer;

	private final String pluginRootNodeQuery;

	@Inject
	public StatusService(@UserClient @Nullable MeshRestClient userClient, SchemaInitializer schemaInitializer) {
		this.userClient = userClient;
		this.schemaInitializer = schemaInitializer;

		try {
			pluginRootNodeQuery = IOUtils.toString(FormsPlugin.class.getResource("forms_plugin_node.graphql"), "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Check whether the project contains the plugin root node, and whether it is published
	 * @param projectName project name
	 * @return single activation response
	 */
	public Single<StatusResponse> check(String projectName) {
		return userClient.graphqlQuery(projectName, pluginRootNodeQuery).toSingle().map(response -> {
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
}
