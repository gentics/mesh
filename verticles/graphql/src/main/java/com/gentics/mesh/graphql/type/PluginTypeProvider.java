
package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class PluginTypeProvider extends AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(PluginTypeProvider.class);

	public static final String PLUGIN_TYPE_NAME = "Plugin";
	public static final String PLUGIN_PAGE_TYPE_NAME = "PluginPage";

	private final MeshPluginManager manager;

	@Inject
	public PluginTypeProvider(AbstractMeshOptions options, MeshPluginManager manager) {
		super(options);
		this.manager = manager;
	}

	public GraphQLFieldDefinition createPluginField() {
		return newFieldDefinition().name("plugin").description("Load plugin by id").argument(createIdArg("Id of the plugin."))
			.type(new GraphQLTypeReference(PLUGIN_TYPE_NAME)).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				if (!gc.getUser().isAdmin()) {
					return new PermissionException("plugins", "Missing admin permission");
				}
				String id = env.getArgument("id");
				if (id == null) {
					return null;
				}
				PluginWrapper pluginWrapper = manager.getPlugin(id);
				if (pluginWrapper == null) {
					return null;
				}
				Plugin p = pluginWrapper.getPlugin();
				if (p instanceof MeshPlugin) {
					return p;
				} else {
					log.warn("The found plugin is not a Gentics Mesh Plugin");
				}
				return null;
			}).build();
	}

	public GraphQLFieldDefinition createPluginPageField() {
		return newFieldDefinition().name("plugins").description("Load plugins").argument(createPagingArgs())
			.type(new GraphQLTypeReference(PLUGIN_PAGE_TYPE_NAME)).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				if (!gc.getUser().isAdmin()) {
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

		// .id
		root.field(newFieldDefinition().name("id").description("The deployment id of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.id();
		}));

		// .name
		root.field(newFieldDefinition().name("name").description("The name of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.name();
		}));

		// .description
		root.field(newFieldDefinition().name("description").description("The description of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getDescription();
		}));

		// .apiName
		root.field(newFieldDefinition().name("apiName").description("The apiName of the plugin").type(GraphQLString).dataFetcher((env) -> {
			MeshPlugin plugin = env.getSource();
			if (plugin instanceof RestPlugin) {
				return ((RestPlugin) plugin).restApiName();
			}
			if (plugin instanceof GraphQLPlugin) {
				return ((GraphQLPlugin) plugin).gqlApiName();
			}
			return null;
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

		// .status
		root.field(newFieldDefinition().name("status").description("The status of the plugin.").type(GraphQLString).dataFetcher(env -> {
			MeshPlugin plugin = env.getSource();
			PluginStatus status = manager.getStatus(plugin.id());
			return status == null ? null : status.name();
		}));

		// .version
		root.field(newFieldDefinition().name("version").description("The version of the plugin").type(GraphQLString).dataFetcher(env -> {
			MeshPlugin plugin = env.getSource();
			return plugin.getManifest().getVersion();
		}));

		return root.build();
	}

	/**
	 * Return a new argument for the id.
	 * 
	 * @param description
	 * @return
	 */
	public GraphQLArgument createIdArg(String description) {
		return newArgument().name("id").type(GraphQLString).description(description).build();
	}
}
