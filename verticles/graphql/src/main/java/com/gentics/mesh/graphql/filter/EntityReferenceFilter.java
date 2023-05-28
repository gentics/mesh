package com.gentics.mesh.graphql.filter;

import static graphql.Scalars.GraphQLBoolean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.ComparisonOperation;
import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.FilterQuery;
import com.gentics.graphqlfilter.filter.operation.UnformalizableQuery;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibReferenceField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.operation.EntityReferenceOperationOperand;

public class EntityReferenceFilter<E extends HibElement, T extends HibReferenceField<E>, Q> extends MainFilter<T> {

	private static Map<String, EntityReferenceFilter<HibNode, HibNodeField, ?>> nodeFieldFilterInstances = new HashMap<>();
	private static Map<String, EntityReferenceFilter<HibMicronode, HibMicronodeField, ?>> micronodeFieldFilterInstances = new HashMap<>();

	private final Filter<E, Q> referenceFilter;
	private final String referenceType;

	public EntityReferenceFilter(String name, String description, String entityType, Filter<E, Q> referenceFilter, Optional<String> ownerType) {
		super(name, description, true, ownerType);
		this.referenceFilter = referenceFilter;
		this.referenceType = entityType;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = new ArrayList<>();
		filters.add(FilterField.<T, Boolean>create("isNull", "Tests if the value is null", GraphQLBoolean, 
				query -> value -> query == (value == null), 
				Optional.of((query) -> wrapReferencingEdgeFilter("isNull", query, FilterField.isNull() )), false, getOwner()));
		filters.add(FilterField.<T, Q>create(referenceType, "Checks if any list item does not match the given predicate", referenceFilter.getType(), 
				query -> val -> val != null && referenceFilter.createPredicate(query).test(val.getReferencedEntity()), 
				Optional.of((query) -> wrapReferencedEntity(query)), true));
		return filters;
	}

	protected final <I, QQ> FilterField<T, ?> makeWrappedFieldFilter(String filterName, String description, Filter<I, QQ> edgeTypeFilter, Function<T, I> mapper) {
		return FilterField.<T, QQ>create(filterName, description, edgeTypeFilter.getType(), 
				query -> val -> val != null && edgeTypeFilter.createPredicate(query).test(mapper.apply(val)), 
				Optional.of((query) -> wrapReferencingEdgeFilter(filterName, query, edgeTypeFilter)), true, getOwner());
	}

	protected final <I> FilterOperation<?> wrapReferencingEdgeFilter(String fieldName, FilterQuery<?, I> query, Filter<?, I> edgeFilter) {
		FilterOperation<?> filterOperation;
		try {
			filterOperation = edgeFilter.createFilterOperation(query).maybeSetFilterId(maybeGetFilterId());
		} catch (UnformalizableQuery e) {
			throw new IllegalArgumentException(e);
		}
		return new ComparisonOperation() {
			
			@Override
			public String getOperator() {
				return StringUtils.EMPTY;
			}
			
			@Override
			public FilterOperand<?> getRight() {
				return new EntityReferenceOperationOperand(filterOperation, referenceType, fieldName, getOwner());
			}
			
			@Override
			public FilterOperand<?> getLeft() {
				return FilterOperand.noOp();
			}

			@Override
			public String getInitiatingFilterName() {
				return query.getInitiatingFilterName();
			}

			@Override
			public Optional<String> maybeGetFilterId() {
				return filterOperation.maybeGetFilterId();
			}
		};
	}

	protected final FilterOperation<?> wrapReferencedEntity(FilterQuery<?, Q> query) {
		return wrapReferencingEdgeFilter(referenceType, query, referenceFilter);
	}

	public static final EntityReferenceFilter<HibNode, HibNodeField, ?> nodeFieldFilter(GraphQLContext context, String owner) {	
		String branchUuid;
		if (StringUtils.isBlank(context.getVersioningParameters().getBranch())) {
			HibProject project = Tx.get().getProject(context);
			branchUuid = project.getLatestBranch().getUuid();
		} else {
			branchUuid = context.getVersioningParameters().getBranch();
		}
		List<String> languageTags = context.getNodeParameters().getLanguageList(CommonTx.get().data().options());
		return nodeFieldFilterInstances.computeIfAbsent(owner, o -> new EntityReferenceFilter<>("NodeFieldBaseFilter", "Filters node field", "node", new MappedFilter<>("NODE", "content", "Filters over field node content", 
				NodeFilter.filter(context), fieldNode -> {
					System.err.println("node " + fieldNode);
					if (fieldNode == null) { 
						return null;
					} else {	
						ContainerType ctype = ContainerType.PUBLISHED;
						HibNodeFieldContainer content = Tx.get().contentDao().findVersion(fieldNode, languageTags, branchUuid, ctype.getHumanCode());
						if (content == null) {
							ctype = ContainerType.forVersion(context.getVersioningParameters().getVersion());
							content = Tx.get().contentDao().findVersion(fieldNode, languageTags, branchUuid, ctype.getHumanCode());
						}						
						return new NodeContent(fieldNode, content, languageTags, ctype);
					} }), Optional.of(o)));
	}

	public static final EntityReferenceFilter<HibMicronode, HibMicronodeField, ?> micronodeFieldFilter(GraphQLContext context, String owner) {
		return micronodeFieldFilterInstances.computeIfAbsent(owner, o -> new EntityReferenceFilter<>("MicronodeFieldBaseFilter", "Filters micronode field", "micronode", MicronodeFilter.filter(context), Optional.of(o)));
	}
}
