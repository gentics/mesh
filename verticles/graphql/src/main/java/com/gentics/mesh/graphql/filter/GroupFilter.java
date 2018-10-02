package com.gentics.mesh.graphql.filter;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.core.data.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupFilter extends StartMainFilter<Group> {

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
	protected List<FilterField<Group, ?>> getFilters() {
		List<FilterField<Group, ?>> filters = new ArrayList<>();
		filters.addAll(CommonFields.nameFilter());
		filters.addAll(CommonFields.userTrackingFilter());
		return filters;
	}
}
