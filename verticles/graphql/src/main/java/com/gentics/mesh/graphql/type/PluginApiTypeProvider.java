package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.plugin.GraphQLPluginRegistry;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PluginApiTypeProvider extends AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(PluginApiTypeProvider.class);

	public static final String PLUGIN_API_TYPE_NAME = "PluginAPIType";

	private final GraphQLPluginRegistry pluginTypeRegistry;

	private final PluginEnvironment pluginEnv;

	@Inject
	public PluginApiTypeProvider(MeshOptions options, GraphQLPluginRegistry pluginTypeRegistry, PluginEnvironment env) {
		super(options);
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

			GraphQLSchema pluginSchema = currentPlugin.createRootSchema();
			if (pluginSchema != null) {
				String apiName = currentPlugin.gqlApiName();
				GraphQLObjectType queryType = pluginSchema.getQueryType();
				GraphQLObjectType prefixedType = queryType.transform(e -> {
					// Prefix all types to avoid clashing of type names with existing types.
					e.name(apiName + "_" + queryType.getName());
				});

				GraphQLFieldDefinition pluginField = newFieldDefinition()
					.name(currentPlugin.gqlApiName())
					.description("API of plugin: " + currentPlugin.getManifest().getDescription())
					.type(prefixedType)
					.dataFetcher(env -> {
						return pluginEnv;
					}).build();

				root.field(pluginField);
			} else {
				log.debug("Plugin {" + currentPlugin.name() + "} did not provide a schema. Not adding GraphQL plugin field.");
			}
		}

		return root.build();
	}

	/**
	 * Check whether there are any graphql plugins.
	 * 
	 * @return
	 */
	public boolean hasPlugins() {
		return !pluginTypeRegistry.getPlugins().isEmpty();
	}

}
