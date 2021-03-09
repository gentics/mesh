package com.gentics.mesh.graphql.filter;

import java.util.function.Function;
import java.util.function.Predicate;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.core.data.HibFieldContainer;

/**
 * Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
 */
public class FieldMappedFilter<T, Q> extends MappedFilter<HibFieldContainer, T, Q> {
	private final String schemaName;

	/**
	 * Creates a new FieldMappedFilter. Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
	 */
	public FieldMappedFilter(String name, String description, Filter<T, Q> delegate, Function<HibFieldContainer, T> mapper, String schemaName) {
		super(name, description, delegate, mapper);
		this.schemaName = schemaName;
	}

	@Override
	public Predicate<HibFieldContainer> createPredicate(Q query) {
		// Return always true if the node is not of the provided schema.
		Predicate<HibFieldContainer> schemaCheck = node -> !node.getSchemaContainerVersion().getName().equals(schemaName);
		Predicate<HibFieldContainer> predicate = super.createPredicate(query);
		return schemaCheck.or(predicate);
	}
}
