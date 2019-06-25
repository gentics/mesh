package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

/**
 * Filters ingoing node references.
 */
public class NodeReferenceFilter extends StartMainFilter<NodeReferenceIn> {
	private final GraphQLContext context;
	private static final String NAME = "NodeReferenceFilter";

	public static NodeReferenceFilter nodeReferenceFilter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new NodeReferenceFilter(context));
	}

	private NodeReferenceFilter(GraphQLContext context) {
		super("NodeReferenceFilter", "Filters by ingoing node references.");
		this.context = context;
	}

	@Override
	protected List<FilterField<NodeReferenceIn, ?>> getFilters() {
		return Arrays.asList(
			new MappedFilter<>("fieldName", "Filters by the field name that is used to reference this node.", StringFilter.filter(), NodeReferenceIn::getFieldName),
			new MappedFilter<>("micronodeFieldName", "Filters by the micronode field name that is used to reference this node.", StringFilter.filter(), NodeReferenceIn::getMicronodeFieldName),
			new MappedFilter<>("node", "Filters by the node that references this node", NodeFilter.filter(context), NodeReferenceIn::getNode)
		);
	}
}
