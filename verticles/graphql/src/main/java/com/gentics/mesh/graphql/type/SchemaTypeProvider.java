package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.SchemaContainer;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

import java.util.stream.Collectors;

@Singleton
public class SchemaTypeProvider extends AbstractTypeProvider {

	public static final String SCHEMA_TYPE_NAME = "Schema";

	public static final String SCHEMA_PAGE_TYPE_NAME = "SchemasPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public SchemaTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder schemaType = newObject().name(SCHEMA_TYPE_NAME).description("Node schema");
		interfaceTypeProvider.addCommonFields(schemaType);

		schemaType.field(newFieldDefinition().name("name").type(GraphQLString).dataFetcher((env) -> {
			Object source = env.getSource();
			if (source instanceof NamedElement) {
				return ((NamedElement) source).getName();
			}
			return null;
		}));

		schemaType.field(newPagingFieldWithFetcher("projects", "Projects that this schema is assigned to", (env) -> {
			SchemaContainer schema = env.getSource();
			return schema.findReferencedReleases().keySet().stream()
				.map(Release::getProject)
				.distinct()
				.collect(Collectors.toList());
		}, PROJECT_REFERENCE_PAGE_TYPE_NAME));

		schemaType.field(newFieldDefinition().name("isContainer").type(GraphQLBoolean));

		schemaType.field(newFieldDefinition().name("displayField").type(GraphQLString));

		schemaType.field(newFieldDefinition().name("segmentField").type(GraphQLString));

		// TODO add fields

		return schemaType.build();
	}
}
