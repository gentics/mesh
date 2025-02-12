package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.role.HibRole;

/**
 * Filters roles in GraphQl. This filter should be used whenever a list of roles is returned.
 */
public class RoleFilter extends EntityFilter<HibRole> {

	private static final ElementType ELEMENT = ElementType.ROLE;
	private static final String NAME = "RoleFilter";

	private static RoleFilter instance;

	public static synchronized RoleFilter filter() {
		if (instance == null) {
			instance = new RoleFilter();
		}
		return instance;
	}

	private RoleFilter() {
		super(NAME, "Filters roles", Optional.of(ELEMENT.name()));
	}

	@Override
	protected List<FilterField<HibRole, ?>> getFilters() {
		String owner = ELEMENT.name();
		List<FilterField<HibRole, ?>> filters = new ArrayList<>();
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
