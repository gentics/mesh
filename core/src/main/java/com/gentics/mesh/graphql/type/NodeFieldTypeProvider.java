package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.DateTestField;
import com.gentics.mesh.graphql.StringTestField;

import graphql.schema.GraphQLInterfaceType;

@Singleton
public class NodeFieldTypeProvider {

	private StringFieldTypeProvider stringFieldProvider;

	private DateFieldTypeProvider dateFieldProvider;

	@Inject
	public NodeFieldTypeProvider(StringFieldTypeProvider stringFieldProvider, DateFieldTypeProvider dateFieldProvider) {
		this.stringFieldProvider = stringFieldProvider;
		this.dateFieldProvider = dateFieldProvider;
	}

	public GraphQLInterfaceType getFieldType() {
		GraphQLInterfaceType fieldType = newInterface().name("Field").description("Field of the node.")
				.field(newFieldDefinition().name("name").description("The name of the field.").type(GraphQLString).build()).typeResolver(object -> {
					if (object instanceof StringTestField) {
						return stringFieldProvider.getStringFieldType();
					}
					if (object instanceof DateTestField) {
						return dateFieldProvider.getDateFieldType();
					}
					return null;
				}).build();
		return fieldType;
	}
}
