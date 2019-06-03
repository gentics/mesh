package com.gentics.mesh.graphql.filter;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.User;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters users in GraphQl. This filter should be used whenever a list of users is returned.
 */
public class UserFilter extends StartMainFilter<User> {

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
		super(NAME, "Filters users");
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
	protected List<FilterField<User, ?>> getFilters() {
		List<FilterField<User, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.uuidFilter());
		filters.addAll(CommonFields.userTrackingFilter(new UserFilter(true)));
		filters.add(new MappedFilter<>("username", "Filters by username", StringFilter.filter(), User::getUsername));
		filters.add(new MappedFilter<>("firstname", "Filters by first name", StringFilter.filter(), User::getFirstname));
		filters.add(new MappedFilter<>("lastname", "Filters by last name", StringFilter.filter(), User::getLastname));
		filters.add(new MappedFilter<>("emailAddress", "Filters by email address", StringFilter.filter(), User::getEmailAddress));
		filters.add(new MappedFilter<>("forcedPasswordChange", "Filters by forced password change flag", BooleanFilter.filter(), User::isForcedPasswordChange));
		return filters;
	}
}
