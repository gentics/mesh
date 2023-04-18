package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.operation.Comparison;

import graphql.Scalars;

/**
 * Boolean filter, with a null check option
 * 
 * @author plyhun
 *
 */
public class NullableBooleanFilter extends MainFilter<Boolean>{

	private static NullableBooleanFilter instance;

	/**
	 * Get the singleton nullable boolean filter
	 */
	public static synchronized NullableBooleanFilter filter() {
		if (instance == null) {
			instance = new NullableBooleanFilter();
		}
		return instance;
	}

	private NullableBooleanFilter() {
		super("NullableBooleanFilter", "Filters booleans with a null check", false, Optional.empty());
	}

	@Override
	protected List<FilterField<Boolean, ?>> getFilters() {
		return Arrays.asList(
			FilterField.isNull(),
			FilterField.create("equals", "Compares two booleans for equality", Scalars.GraphQLBoolean, 
					query -> query::equals, 
					Optional.of((query) -> Comparison.eq(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true)))));
	}
}

