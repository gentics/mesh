
package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class PluginTypeProvider extends AbstractTypeProvider {

	public static final String PLUGIN_TYPE_NAME = "Plugin";
	public static final String PLUGIN_PAGE_TYPE_NAME = "PluginPage";

	private final MeshPluginManager manager;

	@Inject
	public PluginTypeProvider(MeshPluginManager manager) {
		this.manager = manager;
	}

	public GraphQLFieldDefinition createPluginField() {
		return newFieldDefinition().name("plugin").description("Load plugin by uuid").argument(createUuidArg("Uuid of the plugin."))
			.type(new GraphQLTypeReference(PLUGIN_TYPE_NAME)).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				if (!gc.getUser().hasAdminRole()) {
					return new PermissionException("plugins", "Missing admin permission");
				}
				String uuid = env.getArgument("uuid");
				if (uuid == null) {
					return null;
				}
				return manager.getPlugin(uuid);
			}).build();
	}

	public GraphQLFieldDefinition createPluginPageField() {
		return newFieldDefinition().name("plugins").description("Load plugins").argument(createPagingArgs())
			.type(new GraphQLTypeReference(PLUGIN_PAGE_TYPE_NAME)).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				if (!gc.getUser().hasAdminRole()) {
					return new PermissionException("plugins", "Missing admin permission");
				}
				Map<String, MeshPlugin> deployments = manager.getPluginsMap();
				Page<MeshPlugin> page = new DynamicStreamPageImpl<>(deployments.values().stream(), getPagingInfo(env));
				return page;
			}).build();
	}

	public GraphQLType createType() {
		Builder root = newObject();
		root.name(PLUGIN_TYPE_NAME);
		root.description("Gentics Mesh Plugin");

		// .uuid
		root.field(newFieldDefinition().name("uuid").description("The deployment uuid of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.deploymentID();
		}));

		// .name
		root.field(newFieldDefinition().name("name").description("The name of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getName();
		}));

		// .description
		root.field(newFieldDefinition().name("description").description("The description of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getDescription();
		}));

		// .apiName
		root.field(newFieldDefinition().name("apiName").description("The apiName of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getApiName();
		}));

		// .license
		root.field(newFieldDefinition().name("license").description("The license of the plugin").type(GraphQLString).dataFetcher((env -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getLicense();
		})));

		// .author
		root.field(newFieldDefinition().name("author").description("The author of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getAuthor();
		}));

		// .inception
		root.field(newFieldDefinition().name("inception").description("The inception date of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getInception();
		}));

		// .version
		root.field(newFieldDefinition().name("version").description("The version of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getVersion();
		}));

		return root.build();
	}
}
