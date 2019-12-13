package com.gentics.mesh.graphql.filter;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.mesh.core.data.GraphFieldContainer;

/**
 * Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
 */
public class FieldMappedFilter<T, Q> extends MappedFilter<GraphFieldContainer, T, Q> {
	private final String schemaName;

	/**
	 * Creates a new FieldMappedFilter. Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
	 */
	public FieldMappedFilter(String name, String description, Filter<T, Q> delegate, Function<GraphFieldContainer, T> mapper, String schemaName) {
		super(name, description, delegate, mapper);
		this.schemaName = schemaName;
	}

	@Override
	public Predicate<GraphFieldContainer> createPredicate(Q query) {
		// Leave the node in the end result if there is no content.
		Predicate<GraphFieldContainer> isNull = Objects::isNull;
		// Return always true if the node is not of the provided schema.
		Predicate<GraphFieldContainer> schemaCheck = node -> !node.getSchemaContainerVersion().getName().equals(schemaName);
		Predicate<GraphFieldContainer> predicate = super.createPredicate(query);
		return isNull.or(schemaCheck).or(predicate);
	}
}
