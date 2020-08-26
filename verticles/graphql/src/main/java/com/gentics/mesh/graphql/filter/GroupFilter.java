package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.core.data.group.HibGroup;

/**
 * Filters groups in GraphQl. This filter should be used whenever a list of groups is returned.
 */
public class GroupFilter extends StartMainFilter<HibGroup> {

	private static final String NAME = "GroupFilter";

	private static GroupFilter instance;
	public static synchronized GroupFilter filter() {
		if (instance == null) {
			instance = new GroupFilter();
		}
		return instance;
	}

	private GroupFilter() {
		super(NAME, "Filters groups");
	}

	@Override
	protected List<FilterField<HibGroup, ?>> getFilters() {
		List<FilterField<HibGroup, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibNameFilter());
		filters.add(CommonFields.hibUuidFilter());
		filters.addAll(CommonFields.hibUserTrackingFilter());
		return filters;
	}
}
