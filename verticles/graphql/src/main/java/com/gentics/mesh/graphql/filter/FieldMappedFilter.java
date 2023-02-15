package com.gentics.mesh.graphql.filter;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.util.FilterUtil;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
 */
public class FieldMappedFilter<T, Q> extends MappedFilter<HibFieldContainer, T, Q> {
	private final String schemaName;
	private final FieldTypes fieldType;

	/**
	 * Creates a new FieldMappedFilter. Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
	 */
	public FieldMappedFilter(FieldTypes fieldType, String name, String description, Filter<T, Q> delegate, Function<HibFieldContainer, T> mapper, String schemaName) {
		super("content", name, description, delegate, mapper);
		this.schemaName = schemaName;
		this.fieldType = fieldType;
	}

	@Override
	public Predicate<HibFieldContainer> createPredicate(Q query) {
		// Return always true if the node is not of the provided schema.
		Predicate<HibFieldContainer> schemaCheck = node -> !node.getSchemaContainerVersion().getName().equals(schemaName);
		Predicate<HibFieldContainer> predicate = super.createPredicate(query);
		return schemaCheck.or(predicate);
	}

	/**
	 * Get type of this filtered field.
	 * 
	 * @return
	 */
	public FieldTypes getFieldType() {
		return fieldType;
	}

	@Override
	public Map<String, String> getJoins() {
		return FilterUtil.addFluent(super.getJoins(), Collections.singletonMap(schemaName, fieldType.name().toLowerCase()));
	}
}
