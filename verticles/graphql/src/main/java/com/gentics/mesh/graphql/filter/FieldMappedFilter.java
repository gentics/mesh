package com.gentics.mesh.graphql.filter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.Join;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.graphqlfilter.util.FilterUtil;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;

/**
 * Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
 */
public class FieldMappedFilter<T, Q> extends MappedFilter<HibFieldContainer, T, Q> {
	private final String schemaName;
	private final FieldTypes fieldType;
	private final Optional<FieldTypes> maybeItemType;

	/**
	 * Creates a new FieldMappedFilter. Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
	 */
	public FieldMappedFilter(FieldTypes fieldType, String name, String description, Filter<T, Q> delegate, Function<HibFieldContainer, T> mapper, String schemaName) {
		this(fieldType, name, description, delegate, mapper, schemaName, Optional.empty());
	}

	/**
	 * Creates a new FieldMappedFilter. Same as {@link MappedFilter}, but additionally tests if the input node is of the provided schema.
	 */
	public FieldMappedFilter(FieldTypes fieldType, String name, String description, Filter<T, Q> delegate, Function<HibFieldContainer, T> mapper, String schemaName, Optional<FieldTypes> maybeItemType) {
		super("CONTENT", name, description, delegate, mapper);
		this.schemaName = schemaName;
		this.fieldType = fieldType;
		this.maybeItemType = maybeItemType;
	}

	@Override
	public Predicate<HibFieldContainer> createPredicate(Q query) {
		// Return always true if the node is not of the provided schema.
		Predicate<HibFieldContainer> schemaCheck = node -> node != null && !node.getSchemaContainerVersion().getName().equals(schemaName);
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
	public Set<Join> getJoins() {
		JoinPart fieldTypePart = new JoinPart(schemaName + "." + getName(), fieldType.name().toLowerCase());
		Join mainFieldTypeJoin = new Join(new JoinPart("CONTENT", schemaName), fieldTypePart);
		Set<Join> fieldJoins = maybeItemType
				.map(itemType -> Set.of(mainFieldTypeJoin, new Join(fieldTypePart, new JoinPart(fieldType.name().toLowerCase(), itemType.name().toLowerCase()))))
				.orElseGet(() -> Collections.singleton(mainFieldTypeJoin));
		return FilterUtil.addFluent(super.getJoins(), fieldJoins);
	}

	public Optional<FieldTypes> getMaybeItemType() {
		return maybeItemType;
	}
}
