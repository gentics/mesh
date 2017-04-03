package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import io.vertx.core.impl.launcher.commands.VersionCommand;

@Singleton
public class MeshTypeProvider extends AbstractTypeProvider {

	@Inject
	public Database db;

	@Inject
	public SearchProvider searchProvider;

	@Inject
	public MeshTypeProvider() {
	}

	public GraphQLObjectType createMeshType() {
		Builder root = newObject();
		root.name("Mesh");
		root.description("Mesh version information");
		root.field(newFieldDefinition().name("meshVersion")
				.description("Version of mesh")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return Mesh.getPlainVersion();
				}));

		// .meshNodeId
		root.field(newFieldDefinition().name("meshNodeId")
				.description("Node id of this mesh instance")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return MeshNameProvider.getInstance()
							.getName();
				}));

		// .databaseVendor
		root.field(newFieldDefinition().name("databaseVendor")
				.description("Name of the graph database vendor")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return db.getVendorName();
				}));

		// .databaseVersion
		root.field(newFieldDefinition().name("databaseVersion")
				.description("Version of the used graph database")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return db.getVersion();
				}));

		// .searchVendor
		root.field(newFieldDefinition().name("searchVendor")
				.description("Name of the search index vendor")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return searchProvider.getVendorName();
				}));

		// .searchVersion
		root.field(newFieldDefinition().name("searchVersion")
				.description("Version of the used search index")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return searchProvider.getVersion();
				}));

		// .vertxVersion
		root.field(newFieldDefinition().name("vertxVersion")
				.description("Vert.x version")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					return VersionCommand.getVersion();
				}));
		return root.build();
	}

	public GraphQLFieldDefinition createMeshFieldType() {
		return newFieldDefinition().name("mesh")
				.description("The mesh instance")
				.type(createMeshType())
				.dataFetcher((env) -> {
					return Mesh.mesh();
				})
				.build();
	}

}
