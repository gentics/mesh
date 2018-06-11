package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.graphql.context.GraphQLContext;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

import java.util.stream.Collectors;

@Singleton
public class MicroschemaTypeProvider extends AbstractTypeProvider {

	public static final String MICROSCHEMA_TYPE_NAME = "Microschema";

	public static final String MICROSCHEMA_PAGE_TYPE_NAME = "MicroschemasPage";
	
	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public MicroschemaTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder schemaType = newObject().name(MICROSCHEMA_TYPE_NAME).description("Microschema");
		interfaceTypeProvider.addCommonFields(schemaType);

		// .name
		schemaType.field(newFieldDefinition().name("name").description("Name of the microschema").type(GraphQLString));

		// .version
		schemaType.field(newFieldDefinition().name("version").description("Version of the microschema.").type(GraphQLInt));

		// .description
		schemaType.field(newFieldDefinition().name("description").description("Description of the microschema.").type(GraphQLString));

		schemaType.field(newPagingFieldWithFetcher("projects", "Projects that this schema is assigned to", (env) -> {
			GraphQLContext gc = env.getContext();
			MicroschemaContainer microschema = env.getSource();
			return microschema.findReferencedBranches().keySet().stream()
				.map(Branch::getProject)
				.distinct()
				.filter(it -> gc.getUser().hasPermission(it, GraphPermission.READ_PERM))
				.collect(Collectors.toList());
		}, PROJECT_REFERENCE_PAGE_TYPE_NAME));

		// .fields

		// TODO add fields

		return schemaType.build();
	}

}
