package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.core.data.role.HibRole;

/**
 * Filters roles in GraphQl. This filter should be used whenever a list of roles is returned.
 */
public class RoleFilter extends StartMainFilter<HibRole> {

	private static final String NAME = "RoleFilter";

	private static RoleFilter instance;

	public static synchronized RoleFilter filter() {
		if (instance == null) {
			instance = new RoleFilter();
		}
		return instance;
	}

	private RoleFilter() {
		super(NAME, "Filters roles");
	}

	@Override
	protected List<FilterField<HibRole, ?>> getFilters() {
		List<FilterField<HibRole, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibNameFilter());
		filters.add(CommonFields.hibUuidFilter());
		filters.addAll(CommonFields.hibUserTrackingFilter());
		return filters;
	}
}
