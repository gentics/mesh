package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.ComparisonOperation;
import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.FilterQuery;
import com.gentics.graphqlfilter.filter.operation.LiteralOperand;
import com.gentics.graphqlfilter.filter.operation.UnformalizableQuery;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.operation.ListItemOperationOperand;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

/**
 * List filter.
 * 
 * @author plyhun
 *
 * @param <T> list item type
 * @param <Q> value type
 */
public class ListFilter<T, Q> extends MainFilter<Collection<T>> {

	public static final String OP_COUNT = "count";
	public static final String OP_ALL_MATCH = "allMatch";
	public static final String OP_ANY_MATCH = "anyMatch";
	public static final String OP_NONE_MATCH = "noneMatch";
	public static final String OP_ANY_NOT_MATCH = "anyNotMatch";

	private static ListFilter<HibImageVariant, ?> imageVariantsListFilterInstance;
	private static ListFilter<String, ?> stringListFilterInstance;
	private static ListFilter<String, ?> htmlListFilterInstance;
	private static ListFilter<Number, ?> numberListFilterInstance;
	private static ListFilter<Boolean, ?> booleanListFilterInstance;
	private static ListFilter<Long, ?> dateListFilterInstance;
	private static ListFilter<HibNode, ?> nodeListFilterInstance;
	private static ListFilter<HibMicronode, ?> micronodeListFilterInstance;
	private static ListFilter<HibBinaryField, ?> binaryListFilterInstance;
	private static ListFilter<S3HibBinaryField, ?> s3binaryListFilterInstance;
	private static Map<Byte, ListFilter<NodeReferenceIn, ?>> nodeReferenceListFilterInstances = Collections.synchronizedMap(new HashMap<>());

	private final Filter<T, Q> itemFilter;
	private final boolean referenceItem;

	public ListFilter(String name, String description, Filter<T, Q> itemFilter, Optional<String> ownerType, boolean isReferenceItem) {
		super(name, description, true, ownerType);
		this.itemFilter = itemFilter;
		this.referenceItem = isReferenceItem;
	}

	@Override
	protected List<FilterField<Collection<T>, ?>> getFilters() {
		return Arrays.asList(
				FilterField.isNull(),
				new MappedFilter<>(getOwner().orElse("LIST"), OP_COUNT, "Filter over item count", NumberFilter.filter(), 
						val -> val == null ? 0 : val.size()),
				FilterField.<Collection<T>, Q>create(OP_ALL_MATCH, "Checks if all list items match the given predicate", itemFilter.getType(), 
						query -> val -> val != null && val.stream().allMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.of((query) -> wrap(OP_ALL_MATCH, query)), true),
				FilterField.<Collection<T>, Q>create(OP_ANY_MATCH, "Checks if any list item matches the given predicate", itemFilter.getType(), 
						query -> val -> val != null && val.stream().anyMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.of((query) -> wrap(OP_ANY_MATCH, query)), true),
				FilterField.<Collection<T>, Q>create(OP_NONE_MATCH, "Checks if no list items match the given predicate", itemFilter.getType(), 
						query -> val -> val != null && val.stream().noneMatch(item -> itemFilter.createPredicate(query).test(item)), 
						Optional.of((query) -> wrap(OP_NONE_MATCH, query)), true),
				FilterField.<Collection<T>, Q>create(OP_ANY_NOT_MATCH, "Checks if any list item does not match the given predicate", itemFilter.getType(), 
						query -> val -> val != null && val.stream().anyMatch(item -> !itemFilter.createPredicate(query).test(item)), 
						Optional.of((query) -> wrap(OP_ANY_NOT_MATCH, query)), true)
			);
	}

	protected final FilterOperation<?> wrap(String operation, FilterQuery<?, Q> query) {
		FilterOperation<?> filterOperation;
		try {
			filterOperation = itemFilter.createFilterOperation(query).maybeSetFilterId(maybeGetFilterId());
		} catch (UnformalizableQuery e) {
			throw new IllegalArgumentException(e);
		}
		return new ComparisonOperation() {
			
			@Override
			public String getOperator() {
				switch (operation) {
				case OP_ANY_MATCH: 
				case OP_ANY_NOT_MATCH:
					return "NOT IN";
				case OP_NONE_MATCH:
				case OP_ALL_MATCH: 
					return "IN";
				default: throw new IllegalStateException("Unexpected list operation:" + operation);
				}
			}
			
			@Override
			public FilterOperand<?> getRight() {
				switch (operation) {
				case OP_ANY_MATCH: 
				case OP_NONE_MATCH:
					return new ListItemOperationOperand(filterOperation, getOwner(), false, false, referenceItem);
				case OP_ANY_NOT_MATCH:
					return new ListItemOperationOperand(filterOperation, getOwner(), true, false, referenceItem);
				case OP_ALL_MATCH: 
					return new ListItemOperationOperand(filterOperation, getOwner(), true, true, referenceItem);
				default: throw new IllegalStateException("Unexpected list operation:" + operation);
				}
			}
			
			@Override
			public FilterOperand<?> getLeft() {
				return new LiteralOperand<>(0, false);
			}

			@Override
			public String getInitiatingFilterName() {
				return query.getInitiatingFilterName();
			}
		};
	}

	public boolean isReferenceItem() {
		return referenceItem;
	}

	public static final ListFilter<String, ?> stringListFilter() {
		if (stringListFilterInstance == null) {
			stringListFilterInstance = new ListFilter<>("StringListFilter", "Filters string lists", StringFilter.filter(), Optional.of("STRINGLIST"), false);
		}
		return stringListFilterInstance;
	}

	public static final ListFilter<String, ?> htmlListFilter() {
		if (htmlListFilterInstance == null) {
			htmlListFilterInstance = new ListFilter<>("HtmlListFilter", "Filters HTML lists", StringFilter.filter(), Optional.of("HTMLLIST"), false);
		}
		return htmlListFilterInstance;
	}

	public static final ListFilter<Number, ?> numberListFilter() {
		if (numberListFilterInstance == null) {
			numberListFilterInstance = new ListFilter<>("NumberListFilter", "Filters number lists", NumberFilter.filter(), Optional.of("NUMBERLIST"), false);
		}
		return numberListFilterInstance;
	}

	public static final ListFilter<Boolean, ?> booleanListFilter() {
		if (booleanListFilterInstance == null) {
			booleanListFilterInstance = new ListFilter<>("BooleanListFilter", "Filters boolean lists", BooleanFilter.filter(), Optional.of("BOOLEANLIST"), false);
		}
		return booleanListFilterInstance;
	}

	public static final ListFilter<Long, ?> dateListFilter() {
		if (dateListFilterInstance == null) {
			dateListFilterInstance = new ListFilter<>("DateListFilter", "Filters date lists", DateFilter.filter(), Optional.of("DATELIST"), false);
		}
		return dateListFilterInstance;
	}

	public static final ListFilter<HibNode, ?> nodeListFilter(GraphQLContext context) {
		List<String> languageTags = context.getNodeParameters().getLanguageList(CommonTx.get().data().options());
		if (nodeListFilterInstance == null) {
			nodeListFilterInstance = new ListFilter<>("NodeListFilter", "Filters node lists", new MappedFilter<>("NODE", "content", "Filters over field node content", 
					NodeFilter.filter(context), fieldNode -> {
						if (fieldNode == null) { 
							return null;
						} else {
							// TODO FIXME there should be a way to pick node content more precisely
							return languageTags.stream()
								.map(lang -> Tx.get().contentDao().getFieldContainer(fieldNode, lang))
								.filter(content -> content != null)
								.flatMap(content -> Tx.get().contentDao().getContainerEdges(content))
								.map(edge -> new NodeContent(fieldNode, edge))
								.findAny().orElse(null);							
						}}), Optional.of("NODELIST"), true);
		}
		return nodeListFilterInstance;
	}

	public static final ListFilter<HibMicronode, ?> micronodeListFilter(GraphQLContext context) {
		if (micronodeListFilterInstance == null) {
			micronodeListFilterInstance = new ListFilter<>("MicronodeListFilter", "Filters micronode lists", MicronodeFilter.filter(context), Optional.of("MICRONODELIST"), true);
		}
		return micronodeListFilterInstance;
	}
	public static final ListFilter<HibBinaryField, ?> binaryListFilter(GraphQLContext context) {
		if (binaryListFilterInstance == null) {
			binaryListFilterInstance = new ListFilter<>("BinaryListFilter", "Filters binary lists", BinaryFieldFilter.filter("LIST", context), Optional.of("BINARYLIST"), true);
		}
		return binaryListFilterInstance;
	}
	public static final ListFilter<HibImageVariant, ?> imageVariantListFilter(GraphQLContext context, String owner) {
		if (imageVariantsListFilterInstance == null) {
			imageVariantsListFilterInstance = new ListFilter<>("ImageVariantListFilter", "Filters image variant lists", ImageVariantFilter.filter(owner), Optional.of("IMAGEVARIANTLIST"), true);
		}
		return imageVariantsListFilterInstance;
	}
	public static final ListFilter<S3HibBinaryField, ?> s3binaryListFilter(GraphQLContext context) {
		if (s3binaryListFilterInstance == null) {
			s3binaryListFilterInstance = new ListFilter<>("S3BinaryListFilter", "Filters S3 binary lists", S3BinaryFieldFilter.filter("LIST"), Optional.of("S3BINARYLIST"), true);
		}
		return s3binaryListFilterInstance;
	}

	public static final ListFilter<NodeReferenceIn, ?> nodeReferenceListFilter(GraphQLContext context) {
		return nodeReferenceListFilter(context, true, true, true, true, "");
	}

	public static final ListFilter<NodeReferenceIn, ?> nodeReferenceListFilter(GraphQLContext context, boolean lookupInFields, boolean lookupInLists, boolean lookupInContent, boolean lookupInMicrocontent, String namePrefix) {
		byte features = NodeReferenceFilter.createLookupChange(lookupInFields, lookupInLists, lookupInContent, lookupInMicrocontent);

		return nodeReferenceListFilterInstances.computeIfAbsent(features, 
				lookupFeatures -> {
					NodeReferenceFilter filter = NodeReferenceFilter.nodeReferenceFilter(context, features);
					String description = "Filters node reference lists over: " 
							+ (filter.isLookupInFields()? "[Content reference fields]": "") 
							+ (filter.isLookupInLists()?"[Reference list fields]": "") 
							+ (filter.isLookupInContent()?"[Node content]": "") 
							+ (filter.isLookupInMicrocontent()?"[Micronode content]":"");
					return new ListFilter<>("NodeReferenceListFilter_" + filter.getRawLookupFeatures(), description, filter, Optional.of(features + "REFERENCELIST"), true);
				});
	}
}
