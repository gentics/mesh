package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class BranchTypeProvider extends AbstractTypeProvider {

	public static final String BRANCH_TYPE_NAME = "Branch";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public BranchTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder branchType = newObject().name(BRANCH_TYPE_NAME);
		interfaceTypeProvider.addCommonFields(branchType);

		// .name
		branchType.field(newFieldDefinition().name("name").type(GraphQLString));

		// .migrated
		branchType.field(newFieldDefinition().name("migrated").type(GraphQLBoolean));

		// .latest
		branchType.field(newFieldDefinition().name("latest").type(GraphQLBoolean));

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
		branchType.field(newPagingFieldWithFetcher("schemas", "Load schemas assigned to this branch.",
				this::handleBranchSchemas, SCHEMA_PAGE_TYPE_NAME));

		// .tags
		branchType.field(newFieldDefinition().name("tags").argument(createPagingArgs()).type(new GraphQLTypeReference(TAG_PAGE_TYPE_NAME)).dataFetcher((
			env) -> {
			GraphQLContext gc = env.getContext();
			Branch branch = env.getSource();
			return branch.getTags(gc.getUser(), getPagingInfo(env));
		}));

		return branchType.build();
	}

}
