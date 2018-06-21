package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class BranchTypeProvider extends AbstractTypeProvider {

	public static final String RELEASE_TYPE_NAME = "Branch";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public BranchTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder branchType = newObject().name(RELEASE_TYPE_NAME);
		interfaceTypeProvider.addCommonFields(branchType);

		// .name
		branchType.field(newFieldDefinition().name("name").type(GraphQLString));

		// .migrated
		branchType.field(newFieldDefinition().name("migrated").type(GraphQLBoolean));

		// .schema
		branchType.field(
			newFieldDefinition().name("schema").description("Load schema by name or uuid.")
				.argument(createUuidArg("Uuid of the schema."))
				.argument(createNameArg("Name of the schema."))
				.type(new GraphQLTypeReference(SCHEMA_TYPE_NAME))
				.dataFetcher(this::handleBranchSchema)
				.build()
		);

		// .schemas
		branchType.field(newPagingFieldWithFetcher("schemas", "Load schemas assigned to this release.",
				this::handleReleaseSchemas, SCHEMA_PAGE_TYPE_NAME));

		return branchType.build();
	}

}
