package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.user.HibUser;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;

/**
 * Filters users in GraphQl. This filter should be used whenever a list of users is returned.
 */
public class UserFilter extends StartMainFilter<HibUser> {

	private static final String NAME = "UserFilter";

	private static UserFilter instance;
	public static synchronized UserFilter filter() {
		if (instance == null) {
			instance = new UserFilter();
		}
		return instance;
	}

	private final boolean byRef;

	private UserFilter() {
		this(false);
	}

	private UserFilter(boolean byRef) {
		super(NAME, "Filters users", Optional.of(ElementType.USER.name()));
		this.byRef = byRef;
	}

	@Override
	public GraphQLInputType getType() {
		if (byRef) {
			return GraphQLTypeReference.typeRef(NAME);
		} else {
			return super.getType();
		}
	}

	@Override
	protected List<FilterField<HibUser, ?>> getFilters() {
		String owner = ElementType.USER.name();
		List<FilterField<HibUser, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner, new UserFilter(true)));
		filters.add(new MappedFilter<>(owner, "username", "Filters by username", StringFilter.filter(), HibUser::getUsername));
		filters.add(new MappedFilter<>(owner, "firstname", "Filters by first name", StringFilter.filter(), HibUser::getFirstname));
		filters.add(new MappedFilter<>(owner, "lastname", "Filters by last name", StringFilter.filter(), HibUser::getLastname));
		filters.add(new MappedFilter<>(owner, "emailAddress", "Filters by email address", StringFilter.filter(), HibUser::getEmailAddress));
		filters.add(new MappedFilter<>(owner, "forcedPasswordChange", "Filters by forced password change flag", BooleanFilter.filter(), HibUser::isForcedPasswordChange));
		return filters;
	}
}
