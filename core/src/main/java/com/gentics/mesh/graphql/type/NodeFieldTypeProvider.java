package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.graphql.DateTestField;
import com.gentics.mesh.graphql.StringTestField;

import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class NodeFieldTypeProvider {

	private StringFieldTypeProvider stringFieldProvider;

	private DateFieldTypeProvider dateFieldProvider;

	@Inject
	public NodeFieldTypeProvider(StringFieldTypeProvider stringFieldProvider, DateFieldTypeProvider dateFieldProvider) {
		this.stringFieldProvider = stringFieldProvider;
		this.dateFieldProvider = dateFieldProvider;
	}

	public GraphQLInterfaceType getFieldsType() {
		GraphQLInterfaceType fieldType = newInterface().name("Fields").description("Fields of the node.")
				//.field(newFieldDefinition().name("name").description("The name of the field.").type(GraphQLString).build())
				.typeResolver(object -> {
					//TODO determine which node type it is and return the corresponding generated field type.
					// Inspect the node's schema.
//					if (object instanceof StringTestField) {
//						return stringFieldProvider.getStringFieldType();
//					}
//					if (object instanceof DateTestField) {
//						return dateFieldProvider.getDateFieldType();
//					}
					return null;
				}).build();
		return fieldType;
	}

	public GraphQLObjectType generateFieldType(SchemaContainerVersion version) {
		Schema schema  = version.getSchema();
		Builder root = newObject();
		for(FieldSchema fieldSchema : schema.getFields()) {
			root.field(newFieldDefinition().name(fieldSchema.getName()).description(fieldSchema.getLabel()).type(GraphQLString).build());
		}
		return root.build();
	}

}
