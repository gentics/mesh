package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.DateFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUserTracking;

import graphql.util.Pair;

/**
 * Filters tags in GraphQl. This filter should be used whenever a list of tags is returned.
 */
public class TagFilter extends EntityFilter<HibTag> {

	private static final ElementType ELEMENT = ElementType.TAG;
	private static final String NAME = "TagFilter";

	private static TagFilter instance;

	public static synchronized TagFilter filter() {
		if (instance == null) {
			instance = new TagFilter();
		}
		return instance;
	}

	private TagFilter() {
		super(NAME, "Filters tags", Optional.of(ELEMENT.name()));
	}

	@Override
	protected List<FilterField<HibTag, ?>> getFilters() {
		String owner = ELEMENT.name();
		List<FilterField<HibTag, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibNameFilter(owner));
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner));
		filters.add(new MappedFilter<>(owner, "tagFamily", "Filters by creator", TagFamilyFilter.filter(), HibTag::getTagFamily, Pair.pair("tagFamily", new JoinPart(ElementType.USER.name(), "uuid"))));
		return filters;
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}
}
