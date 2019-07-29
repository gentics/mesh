package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.plugin.PluginTypeRegistry;
import com.gentics.mesh.plugin.GraphQLPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PluginApiTypeProvider extends AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(PluginApiTypeProvider.class);

	public static final String PLUGIN_API_TYPE_NAME = "PluginAPIType";

	private final PluginTypeRegistry pluginTypeRegistry;

	private final PluginEnvironment pluginEnv;

	@Inject
	public PluginApiTypeProvider(PluginTypeRegistry pluginTypeRegistry, PluginEnvironment env) {
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
				.name(currentPlugin.id())
				.description("API of plugin: " + currentPlugin.getManifest().getDescription())
				.type(currentPlugin.createType())
				.dataFetcher(env -> {
					return pluginEnv;
				}).build();

			root.field(pluginField);
		}

		return root.build();
	}

}
