package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.group.Group;

/**
 * Filters groups in GraphQl. This filter should be used whenever a list of groups is returned.
 */
public class GroupFilter extends EntityFilter<Group> {

	private static final ElementType ELEMENT = ElementType.GROUP;
	private static final String NAME = "GroupFilter";

	private static GroupFilter instance;

	/**
	 * Create a new group filter.
	 * 
	 * @return
	 */
	public static synchronized GroupFilter filter() {
		if (instance == null) {
			instance = new GroupFilter();
		}
		return instance;
	}

	private GroupFilter() {
		super(NAME, "Filters groups", Optional.of(ELEMENT.name()));
	}

	@Override
	protected List<FilterField<Group, ?>> getFilters() {
		String owner = ELEMENT.name();
		List<FilterField<Group, ?>> filters = new ArrayList<>();
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
