package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;

/**
 * Filters tag families in GraphQl. This filter should be used whenever a list of tag families is returned.
 */
public class TagFamilyFilter extends EntityFilter<HibTagFamily> {

	private static final ElementType ELEMENT = ElementType.TAGFAMILY;
	private static final String NAME = "TagFamilyFilter";

	private static TagFamilyFilter instance;

	public static synchronized TagFamilyFilter filter() {
		if (instance == null) {
			instance = new TagFamilyFilter();
		}
		return instance;
	}

	private TagFamilyFilter() {
		super(NAME, "Filters tag families", Optional.of(ELEMENT.name()));
	}

	@Override
	protected List<FilterField<HibTagFamily, ?>> getFilters() {
		String owner = ELEMENT.name();
		List<FilterField<HibTagFamily, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibNameFilter(owner));
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner));
		return filters;
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}
}
