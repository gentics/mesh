package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.Scalars;
import graphql.schema.GraphQLList;

/**
 * List filter.
 * 
 * @author plyhun
 *
 * @param <T> list item type
 * @param <Q> value type
 */
public class ListFilter<T, Q> extends MainFilter<Collection<T>> {

	private static ListFilter<String, ?> stringListFilterInstance;
	private static ListFilter<BigDecimal, ?> numberListFilterInstance;
	private static ListFilter<Boolean, ?> booleanListFilterInstance;
	private static ListFilter<Long, ?> dateListFilterInstance;
	private static ListFilter<NodeContent, ?> nodeListFilterInstance;
	private static ListFilter<HibMicronode, ?> micronodeListFilterInstance;

	private final Filter<T, Q> itemFilter;

	public ListFilter(String name, String description, Filter<T, Q> itemFilter, Optional<String> ownerType) {
		super(name, description, true, ownerType);
		this.itemFilter = itemFilter;
	}

	@Override
	protected List<FilterField<Collection<T>, ?>> getFilters() {
		return Arrays.asList(
				FilterField.isNull(),
				FilterField.<Collection<T>, Integer>create("countMoreThan", "Checks if list items count is more than given number", Scalars.GraphQLBoolean, 
						query -> val -> val.size() > query, 
						Optional.empty(), true),
				FilterField.<Collection<T>, Integer>create("countLessThan", "Checks if list items count is less than given number", Scalars.GraphQLBoolean, 
						query -> val -> val.size() < query, 
						Optional.empty(), true),
				FilterField.<Collection<T>, Integer>create("countMoreOrEqualThan", "Checks if list items count is more or equal than given number", Scalars.GraphQLBoolean, 
						query -> val -> val.size() >= query, 
						Optional.empty(), true),
				FilterField.<Collection<T>, Integer>create("countLessOrEqualThan", "Checks if list items count is less or equal than given number", Scalars.GraphQLBoolean, 
						query -> val -> val.size() <= query, 
						Optional.empty(), true),
				FilterField.<Collection<T>, Integer>create("countEquals", "Checks if list items count equals the given number", Scalars.GraphQLBoolean, 
						query -> val -> (val == null && query == 0) ? true : val != null && val.size() == query, 
						Optional.empty()),
				FilterField.<Collection<T>, Integer>create("countNotEquals", "Checks if list items count is not equal the number", Scalars.GraphQLBoolean, 
						query -> val -> (val == null && query != 0) ? true : val != null && val.size() != query, 
						Optional.empty()),
				FilterField.<Collection<T>, Q>create("allMatch", "Checks if all list items match the given predicate", itemFilter.getType(), 
						query -> val -> val.stream().allMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Q>create("anyMatch", "Checks if any list item matches the given predicate", itemFilter.getType(), 
						query -> val -> val.stream().anyMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Collection<Q>>create("anyOf", "Checks if any list item matches the given predicate", GraphQLList.list(itemFilter.getType()), 
						query -> val -> val.stream().filter(item -> !query.stream().anyMatch(qitem -> itemFilter.createPredicate(qitem).test(item))).findAny().isEmpty(), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Collection<Q>>create("allOf", "Checks if every list item matches at least one", GraphQLList.list(itemFilter.getType()), 
						query -> val -> val.stream().filter(item -> !query.stream().allMatch(qitem -> itemFilter.createPredicate(qitem).test(item))).findAny().isEmpty(), 
						Optional.empty(), true)
			);
	}

	public static final ListFilter<String, ?> stringListFilter() {
		if (stringListFilterInstance == null) {
			stringListFilterInstance = new ListFilter<>("StringListFilter", "Filters string lists", StringFilter.filter(), Optional.empty());
		}
		return stringListFilterInstance;
	}

	public static final ListFilter<BigDecimal, ?> numberListFilter() {
		if (numberListFilterInstance == null) {
			numberListFilterInstance = new ListFilter<>("NumberListFilter", "Filters number lists", NumberFilter.filter(), Optional.empty());
		}
		return numberListFilterInstance;
	}

	public static final ListFilter<Boolean, ?> booleanListFilter() {
		if (booleanListFilterInstance == null) {
			booleanListFilterInstance = new ListFilter<>("BooleanListFilter", "Filters boolean lists", BooleanFilter.filter(), Optional.empty());
		}
		return booleanListFilterInstance;
	}

	public static final ListFilter<Long, ?> dateListFilter() {
		if (dateListFilterInstance == null) {
			dateListFilterInstance = new ListFilter<>("DateListFilter", "Filters date lists", DateFilter.filter(), Optional.empty());
		}
		return dateListFilterInstance;
	}

	public static final ListFilter<NodeContent, ?> nodeListFilter(GraphQLContext context) {
		if (nodeListFilterInstance == null) {
			nodeListFilterInstance = new ListFilter<>("NodeListFilter", "Filters node lists", NodeFilter.filter(context), Optional.empty());
		}
		return nodeListFilterInstance;
	}

	public static final ListFilter<HibMicronode, ?> micronodeListFilter(GraphQLContext context) {
		if (micronodeListFilterInstance == null) {
			micronodeListFilterInstance = new ListFilter<>("MicronodeListFilter", "Filters micronode lists", MicronodeFilter.filter(context), Optional.empty());
		}
		return micronodeListFilterInstance;
	}
}
