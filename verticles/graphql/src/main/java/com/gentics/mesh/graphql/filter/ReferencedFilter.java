package com.gentics.mesh.graphql.filter;

import com.gentics.graphqlfilter.filter.NamedFilter;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;

/**
 * Externally/recursively referenced type. Both {@link ReferencedFilter#createType()} and {@link ReferencedFilter#createSortingType()} should be explicitly called on creation.
 * 
 * @author plyhun
 *
 * @param <T>
 * @param <Q>
 */
public interface ReferencedFilter<T, Q> extends NamedFilter<T, Q>{

	@Override
	default GraphQLInputType getType() {
		return GraphQLTypeReference.typeRef(getName());
	}

	@Override
	default GraphQLInputType getSortingType() {
		return GraphQLTypeReference.typeRef(getSortingName());
	}

	/**
	 * Create the type explicitly.
	 * 
	 * @return
	 */
	GraphQLInputType createType();

	/**
	 * Create the sort type explicitly.
	 * 
	 * @return
	 */
	GraphQLInputType createSortingType();
}
