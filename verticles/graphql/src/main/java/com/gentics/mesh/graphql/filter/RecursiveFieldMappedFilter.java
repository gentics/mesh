package com.gentics.mesh.graphql.filter;

import static graphql.schema.GraphQLEnumType.newEnum;

import java.util.Arrays;
import java.util.function.Function;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;

import graphql.schema.GraphQLInputType;

/**
 * An extension to {@link FieldMappedFilter}, with a recursion aware safeguards.
 * 
 * @author plyhun
 *
 * @param <T>
 * @param <Q>
 */
public class RecursiveFieldMappedFilter<T, Q> extends FieldMappedFilter<T, Q> {

	private static final int MAX_LEVEL = 3;

	private static final GraphQLInputType dummyGraphQlType = newEnum().name("Dummy")
			.description("A dummy GraphQL input type, impossible to use. Appearing of it means something that should not be filtered, like a recursive structure, driven too deep.").build();

	public RecursiveFieldMappedFilter(FieldTypes fieldType, String name, String description, Filter<T, Q> delegate,
			Function<HibFieldContainer, T> mapper, String schemaName) {
		super(fieldType, name, description, delegate, mapper, schemaName);
	}

	@Override
	public GraphQLInputType getType() {
		long recursionLevel = Arrays.stream(Thread.currentThread().getStackTrace())
			.filter(ste -> ste.getClassName().equals(getClass().getName()) && "getType".equals(ste.getMethodName()))
			.count();
		if (recursionLevel > MAX_LEVEL) {
			return dummyGraphQlType;
		}		
		return super.getType();
	}
	@Override
	public GraphQLInputType getSortingType() {
		long recursionLevel = Arrays.stream(Thread.currentThread().getStackTrace())
				.filter(ste -> ste.getClassName().equals(getClass().getName()) && "getSortingType".equals(ste.getMethodName()))
				.count();					
			if (recursionLevel > MAX_LEVEL) {
				return dummyGraphQlType;
			}		
			return super.getSortingType();
	}
}
