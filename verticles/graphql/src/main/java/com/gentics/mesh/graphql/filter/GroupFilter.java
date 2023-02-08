package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.group.HibGroup;

/**
 * Filters groups in GraphQl. This filter should be used whenever a list of groups is returned.
 */
public class GroupFilter extends StartMainFilter<HibGroup> {

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
		super(NAME, "Filters groups", Optional.of(ElementType.GROUP.name()));
	}

	@Override
	protected List<FilterField<HibGroup, ?>> getFilters() {
		String owner = ElementType.GROUP.name();
		List<FilterField<HibGroup, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibNameFilter(owner));
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner));
		return filters;
	}
}
