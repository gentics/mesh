package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;
import graphql.util.Pair;

/**
 * Filters incoming node references.
 */
public class NodeReferenceFilter extends StartMainFilter<NodeReferenceIn> implements TypeReferencedFilter<NodeReferenceIn, Map<String, ?>> {
	private final GraphQLContext context;
	private final byte lookupFeatures;
	private static final String NAME = "NodeReferenceFilter";

	public static NodeReferenceFilter nodeReferenceFilter(GraphQLContext context) {
		return nodeReferenceFilter(context, true, true, true, true);
	}

	public static NodeReferenceFilter nodeReferenceFilter(GraphQLContext context, boolean lookupInFields, boolean lookupInLists, boolean lookupInContent, boolean lookupInMicrocontent) {
		byte lookupFeatures = createLookupChange(lookupInFields, lookupInLists, lookupInContent, lookupInMicrocontent);
		return nodeReferenceFilter(context, lookupFeatures);
	}

	public static NodeReferenceFilter nodeReferenceFilter(GraphQLContext context, byte lookupFeatures) {
		return context.getOrStore(NAME + "_" + String.valueOf(lookupFeatures), () -> new NodeReferenceFilter(context, lookupFeatures));
	}

	private NodeReferenceFilter(GraphQLContext context, byte lookupFeatures) {
		super(NAME + "_" + String.valueOf(lookupFeatures), "Filters by incoming node references.", Optional.empty()); // TODO empty?
		this.context = context;
		this.lookupFeatures = lookupFeatures;
	}

	@Override
	protected List<FilterField<NodeReferenceIn, ?>> getFilters() {
		String owner = ElementType.NODE.name();
		return Arrays.asList(
			new MappedFilter<>(owner, "fieldName", "Filters by the field name that is used to reference this node.", StringFilter.filter(), NodeReferenceIn::getFieldName),
			new MappedFilter<>(owner, "micronodeFieldName", "Filters by the micronode field name that is used to reference this node.", StringFilter.filter(), NodeReferenceIn::getMicronodeFieldName),
			new MappedFilter<>(owner, "node", "Filters by the node that references this node", NodeFilter.filter(context), NodeReferenceIn::getNode, Pair.pair("value", new JoinPart("NODE", "uuid")))
		);
	}

	@Override
	public GraphQLInputType getType() {
		return GraphQLTypeReference.typeRef(getName());
	}

	@Override
	public GraphQLInputType getSortingType() {
		return GraphQLTypeReference.typeRef(getSortingName());
	}

	@Override
	public final GraphQLInputType createType() {
		return super.getType();
	}

	@Override
	public final GraphQLInputType createSortingType() {
		return super.getSortingType();
	}

	@Override
	public boolean isSortable() {
		return false;
	}

	public boolean isLookupInFields() {
		return isLookupInFields(lookupFeatures);
	}

	public boolean isLookupInLists() {
		return isLookupInLists(lookupFeatures);
	} 

	public boolean isLookupInContent() {
		return isLookupInContent(lookupFeatures);
	} 

	public boolean isLookupInMicrocontent() {
		return isLookupInMicrocontent(lookupFeatures);
	}

	public byte getRawLookupFeatures() {
		return lookupFeatures;
	}

	public static boolean isLookupInFields(byte currentValue) {
		return (currentValue & 0b1) > 0;
	}

	public static boolean isLookupInLists(byte currentValue) {
		return (currentValue & 0b10) > 0;
	} 

	public static boolean isLookupInContent(byte currentValue) {
		return (currentValue & 0b100) > 0;
	} 

	public static boolean isLookupInMicrocontent(byte currentValue) {
		return (currentValue & 0b1000) > 0;
	}

	public static byte createLookupChange(boolean lookupInFields, boolean lookupInLists, boolean lookupInContent, boolean lookupInMicrocontent) {
		return applyLookupChange((byte) 0, lookupInFields, lookupInLists, lookupInContent, lookupInMicrocontent);
	}

	public static byte applyLookupChange(byte currentValue, boolean lookupInFields, boolean lookupInLists, boolean lookupInContent, boolean lookupInMicrocontent) {
		if (lookupInFields) {
			currentValue |= 0b1;
		}
		if (lookupInLists) {
			currentValue |= 0b10;
		}
		if (lookupInContent) {
			currentValue |= 0b100;
		}
		if (lookupInMicrocontent) {
			currentValue |= 0b1000;
		}
		return currentValue;
	}
}
