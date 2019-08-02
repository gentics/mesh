package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.plugin.GraphQLPluginRegistry;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;

@Singleton
public class PluginApiTypeProvider extends AbstractTypeProvider {

	public static final String PLUGIN_API_TYPE_NAME = "PluginAPIType";

	private final GraphQLPluginRegistry pluginTypeRegistry;

	private final PluginEnvironment pluginEnv;

	@Inject
	public PluginApiTypeProvider(GraphQLPluginRegistry pluginTypeRegistry, PluginEnvironment env) {
		this.pluginTypeRegistry = pluginTypeRegistry;
		this.pluginEnv = env;
	}

	public GraphQLFieldDefinition createPluginAPIField() {
		return newFieldDefinition().name("pluginApi")
			.description("Access API exposed by Gentics Mesh plugins")
			.type(createType())
			.dataFetcher(env -> {
				return pluginEnv;
			}).build();
	}

	public GraphQLOutputType createType() {
		Builder root = newObject();
		root.name(PLUGIN_API_TYPE_NAME);
		root.description("The Plugin API");

		for (GraphQLPlugin currentPlugin : pluginTypeRegistry.getPlugins()) {
			GraphQLFieldDefinition pluginField = newFieldDefinition()
				.name(currentPlugin.gqlApiName())
				.description("API of plugin: " + currentPlugin.getManifest().getDescription())
				.type(currentPlugin.createRootSchema().getQueryType())
				.dataFetcher(env -> {
					return pluginEnv;
				}).build();

			root.field(pluginField);
		}

		return root.build();
	}

}
