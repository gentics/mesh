package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.search.SearchProvider;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;
import io.vertx.core.impl.launcher.commands.VersionCommand;

@Singleton
public class MeshTypeProvider {

	public static final String MESH_TYPE_NAME = "Mesh";

	private final Database db;

	private final SearchProvider searchProvider;

	private final BootstrapInitializer boot;

	private final LocalConfigApi localConfigApi;

	private final MeshOptions options;

	@Inject
	public MeshTypeProvider(BootstrapInitializer boot, Database db, SearchProvider searchProvider, LocalConfigApi localConfigApi, MeshOptions options) {
		this.boot = boot;
		this.db = db;
		this.searchProvider = searchProvider;
		this.localConfigApi = localConfigApi;
		this.options = options;
	}

	public GraphQLObjectType createType() {
		Builder root = newObject();
		root.name(MESH_TYPE_NAME);
		root.description("Mesh version information");
		root.field(newFieldDefinition().name("meshVersion").description("Version of mesh").type(GraphQLString).dataFetcher((env) -> {
			if (isTokenAllowed(env)) {
				return Mesh.getPlainVersion();
			} else {
				return null;
			}
		}));

		// .meshNodeId
		root.field(newFieldDefinition().name("meshNodeId").description("Node id of this mesh instance").type(GraphQLString)
			.dataFetcher((env) -> {
				if (isTokenAllowed(env)) {
					return boot.mesh().getOptions().getNodeName();
				} else {
					return null;
				}
			}));

		// .databaseVendor
		root.field(newFieldDefinition().name("databaseVendor").description("Name of the graph database vendor").type(GraphQLString)
				.dataFetcher((env) -> {
					if (isTokenAllowed(env)) {
						return db.getVendorName();
					} else {
						return null;
					}
				}));

		// .databaseVersion
		root.field(newFieldDefinition().name("databaseVersion").description("Version of the used graph database").type(GraphQLString)
				.dataFetcher(env -> {
					if (isTokenAllowed(env)) {
						return db.getVersion();
					} else {
						return null;
					}
				}));

		// .searchVendor
		root.field(newFieldDefinition().name("searchVendor").description("Name of the search index vendor").type(GraphQLString).dataFetcher((env) -> {
			return searchProvider.getVendorName();
		}));

		// .searchVersion
		root.field(
				newFieldDefinition().name("searchVersion").description("Version of the used search index").type(GraphQLString).dataFetcher(env -> {
					if (isTokenAllowed(env)) {
						return searchProvider.getVersion(true);
					} else {
						return null;
					}
				}));

		// .vertxVersion
		root.field(newFieldDefinition().name("vertxVersion").description("Vert.x version").type(GraphQLString).dataFetcher((env) -> {
			if (isTokenAllowed(env)) {
				return VersionCommand.getVersion();
			} else {
				return null;
			}
		}));

		root.field(newFieldDefinition().name("config").description("The local configuration of this instance")
			.type(new GraphQLObjectType.Builder()
				.name("Configuration")
				.description("The local configuration of this instance")
				.field(new GraphQLFieldDefinition.Builder()
					.name("readOnly")
					.description("If true, Gentics Mesh currently runs in read only mode.")
					.type(nonNull(GraphQLBoolean))
				)
			)
			.dataFetcher(env -> localConfigApi.getActiveConfig().blockingGet()));

		return root.build();
	}

	private boolean isTokenAllowed(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		return options.getHttpServerOptions().isServerTokens() || gc.isAdmin();
	}

	public GraphQLFieldDefinition createMeshFieldType() {
		return newFieldDefinition().name("mesh").description("The mesh instance").type(new GraphQLTypeReference(MESH_TYPE_NAME))
				.dataFetcher((env) -> {
					return boot.mesh();
				}).build();
	}

}
