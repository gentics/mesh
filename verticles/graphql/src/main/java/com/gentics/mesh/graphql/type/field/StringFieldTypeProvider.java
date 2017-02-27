package com.gentics.mesh.graphql.type.field;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;

import graphql.schema.GraphQLFieldDefinition;

@Singleton
public class StringFieldTypeProvider {

	@Inject
	public StringFieldTypeProvider() {
	}

	public GraphQLFieldDefinition getFieldDefinition(String fieldName, String description) {
		return newFieldDefinition().name(fieldName).description(description).type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					Object context = fetcher.getContext();
					if (source instanceof NodeGraphFieldContainer) {
						System.out.println(context.getClass().getName());
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getString(fieldName).getString();
					}
					return null;
				}).build();
	}

}
