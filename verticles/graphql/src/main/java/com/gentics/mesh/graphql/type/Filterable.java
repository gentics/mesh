package com.gentics.mesh.graphql.type;

import java.util.Map;
import java.util.function.Predicate;

import com.gentics.mesh.core.data.root.RootVertex;
import com.tinkerpop.blueprints.Vertex;

import graphql.schema.GraphQLArgument;

public interface Filterable {

	/**
	 * Construct and return the filter argument for the specific type.
	 * 
	 * @return
	 */
	GraphQLArgument createFilterArgument();

	/**
	 * Construct a filter for the type and return a predicate which can be used to filter vertices.
	 * 
	 * @param filter
	 * @param root
	 * @return
	 */
	Predicate<Vertex> constructFilter(Map<String, Object> filter, RootVertex<?> root);

}
