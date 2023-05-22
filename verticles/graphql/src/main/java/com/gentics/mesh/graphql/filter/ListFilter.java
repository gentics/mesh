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
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

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
	private static ListFilter<String, ?> htmlListFilterInstance;
	private static ListFilter<BigDecimal, ?> numberListFilterInstance;
	private static ListFilter<Boolean, ?> booleanListFilterInstance;
	private static ListFilter<Long, ?> dateListFilterInstance;
	private static ListFilter<NodeContent, ?> nodeListFilterInstance;
	private static ListFilter<HibMicronode, ?> micronodeListFilterInstance;
	private static ListFilter<HibBinaryField, ?> binaryListFilterInstance;
	private static ListFilter<S3HibBinaryField, ?> s3binaryListFilterInstance;
	private static ListFilter<NodeReferenceIn, ?> nodeReferenceListFilterInstance;

	private final Filter<T, Q> itemFilter;

	public ListFilter(String name, String description, Filter<T, Q> itemFilter, Optional<String> ownerType) {
		super(name, description, true, ownerType);
		this.itemFilter = itemFilter;
	}

	@Override
	protected List<FilterField<Collection<T>, ?>> getFilters() {
		return Arrays.asList(
				FilterField.isNull(),
				new MappedFilter<>("LIST", "count", "Filter over item count", NumberFilter.filter(), val -> val == null ? BigDecimal.ZERO : new BigDecimal(val.size())),

				FilterField.<Collection<T>, Q>create("allItemsMatch", "Checks if all list items match the given predicate", itemFilter.getType(), 
						query -> val -> val != null && val.stream().allMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Q>create("anyItemMatch", "Checks if any list item matches the given predicate", itemFilter.getType(), 
						query -> val -> val != null && val.stream().anyMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Collection<Q>>create("anyItemMatchAllOf", "Checks if any list item matches all the given predicates", GraphQLList.list(itemFilter.getType()), 
						query -> val -> val != null && val.stream().anyMatch(item -> query.stream().allMatch(qitem -> itemFilter.createPredicate(qitem).test(item))), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Collection<Q>>create("allItemsMatchAllOf", "Checks if every list item matches all the given predicates", GraphQLList.list(itemFilter.getType()), 
						query -> val -> val != null && val.stream().allMatch(item -> query.stream().allMatch(qitem -> itemFilter.createPredicate(qitem).test(item))), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Collection<Q>>create("anyItemMatchAnyOf", "Checks if any list item matches any of the given predicates", GraphQLList.list(itemFilter.getType()), 
						query -> val -> val != null && val.stream().anyMatch(item -> query.stream().anyMatch(qitem -> itemFilter.createPredicate(qitem).test(item))), 
						Optional.empty(), true),
				FilterField.<Collection<T>, Collection<Q>>create("allItemsMatchAnyOf", "Checks if every list item matches any of the given predicates", GraphQLList.list(itemFilter.getType()), 
						query -> val -> val != null && val.stream().allMatch(item -> query.stream().anyMatch(qitem -> itemFilter.createPredicate(qitem).test(item))), 
						Optional.empty(), true)
			);
	}

	public static final ListFilter<String, ?> stringListFilter() {
		if (stringListFilterInstance == null) {
			stringListFilterInstance = new ListFilter<>("StringListFilter", "Filters string lists", StringFilter.filter(), Optional.of("STRINGLIST"));
		}
		return stringListFilterInstance;
	}

	public static final ListFilter<String, ?> htmlListFilter() {
		if (htmlListFilterInstance == null) {
			htmlListFilterInstance = new ListFilter<>("HtmlListFilter", "Filters HTML lists", StringFilter.filter(), Optional.of("HTMLLIST"));
		}
		return htmlListFilterInstance;
	}

	public static final ListFilter<BigDecimal, ?> numberListFilter() {
		if (numberListFilterInstance == null) {
			numberListFilterInstance = new ListFilter<>("NumberListFilter", "Filters number lists", NumberFilter.filter(), Optional.of("NUMBERLIST"));
		}
		return numberListFilterInstance;
	}

	public static final ListFilter<Boolean, ?> booleanListFilter() {
		if (booleanListFilterInstance == null) {
			booleanListFilterInstance = new ListFilter<>("BooleanListFilter", "Filters boolean lists", BooleanFilter.filter(), Optional.of("BOOLEANLIST"));
		}
		return booleanListFilterInstance;
	}

	public static final ListFilter<Long, ?> dateListFilter() {
		if (dateListFilterInstance == null) {
			dateListFilterInstance = new ListFilter<>("DateListFilter", "Filters date lists", DateFilter.filter(), Optional.of("DATELIST"));
		}
		return dateListFilterInstance;
	}

	public static final ListFilter<NodeContent, ?> nodeListFilter(GraphQLContext context) {
		if (nodeListFilterInstance == null) {
			nodeListFilterInstance = new ListFilter<>("NodeListFilter", "Filters node lists", NodeFilter.filter(context), Optional.of("NODELIST"));
		}
		return nodeListFilterInstance;
	}

	public static final ListFilter<HibMicronode, ?> micronodeListFilter(GraphQLContext context) {
		if (micronodeListFilterInstance == null) {
			micronodeListFilterInstance = new ListFilter<>("MicronodeListFilter", "Filters micronode lists", MicronodeFilter.filter(context), Optional.of("MICRONODELIST"));
		}
		return micronodeListFilterInstance;
	}
	public static final ListFilter<HibBinaryField, ?> binaryListFilter(GraphQLContext context) {
		if (binaryListFilterInstance == null) {
			binaryListFilterInstance = new ListFilter<>("BinaryListFilter", "Filters binary lists", BinaryFieldFilter.filter("LIST"), Optional.of("BINARYLIST"));
		}
		return binaryListFilterInstance;
	}

	public static final ListFilter<S3HibBinaryField, ?> s3binaryListFilter(GraphQLContext context) {
		if (s3binaryListFilterInstance == null) {
			s3binaryListFilterInstance = new ListFilter<>("S3BinaryListFilter", "Filters S3 binary lists", S3BinaryFieldFilter.filter("LIST"), Optional.of("S3BINARYLIST"));
		}
		return s3binaryListFilterInstance;
	}

	public static final ListFilter<NodeReferenceIn, ?> nodeReferenceListFilter(GraphQLContext context) {
		if (nodeReferenceListFilterInstance == null) {
			nodeReferenceListFilterInstance = new ListFilter<>("NodeReferenceListFilter", "Filters node reference lists", NodeReferenceFilter.nodeReferenceFilter(context), Optional.of("REFERENCELIST"));
		}
		return nodeReferenceListFilterInstance;
	}
}
