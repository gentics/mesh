package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;
import io.vertx.core.impl.launcher.commands.VersionCommand;

@Singleton
public class MeshTypeProvider {

	public static final String MESH_TYPE_NAME = "Mesh";

	private final LegacyDatabase db;

	private final SearchProvider searchProvider;

	@Inject
	public MeshTypeProvider(LegacyDatabase db, SearchProvider searchProvider) {
		this.db = db;
		this.searchProvider = searchProvider;
	}

	public GraphQLObjectType createType() {
		Builder root = newObject();
		root.name(MESH_TYPE_NAME);
		root.description("Mesh version information");
		root.field(newFieldDefinition().name("meshVersion").description("Version of mesh").type(GraphQLString).dataFetcher((env) -> {
			return Mesh.getPlainVersion();
		}));

		// .meshNodeId
		root.field(newFieldDefinition().name("meshNodeId").description("Node id of this mesh instance").type(GraphQLString).dataFetcher((env) -> {
			return Mesh.mesh().getOptions().getNodeName();
		}));

		// .databaseVendor
		root.field(newFieldDefinition().name("databaseVendor").description("Name of the graph database vendor").type(GraphQLString)
				.dataFetcher((env) -> {
					return db.getVendorName();
				}));

		// .databaseVersion
		root.field(newFieldDefinition().name("databaseVersion").description("Version of the used graph database").type(GraphQLString)
				.dataFetcher((env) -> {
					return db.getVersion();
				}));

		// .searchVendor
		root.field(newFieldDefinition().name("searchVendor").description("Name of the search index vendor").type(GraphQLString).dataFetcher((env) -> {
			return searchProvider.getVendorName();
		}));

		// .searchVersion
		root.field(
				newFieldDefinition().name("searchVersion").description("Version of the used search index").type(GraphQLString).dataFetcher((env) -> {
					return searchProvider.getVersion();
				}));

		// .vertxVersion
		root.field(newFieldDefinition().name("vertxVersion").description("Vert.x version").type(GraphQLString).dataFetcher((env) -> {
			return VersionCommand.getVersion();
		}));
		return root.build();
	}

	public GraphQLFieldDefinition createMeshFieldType() {
		return newFieldDefinition().name("mesh").description("The mesh instance").type(new GraphQLTypeReference(MESH_TYPE_NAME))
				.dataFetcher((env) -> {
					return Mesh.mesh();
				}).build();
	}

}
