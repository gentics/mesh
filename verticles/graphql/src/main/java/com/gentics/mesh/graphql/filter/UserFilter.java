package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.BooleanFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.user.User;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;

/**
 * Filters users in GraphQl. This filter should be used whenever a list of users is returned.
 */
public class UserFilter extends EntityFilter<User> implements TypeReferencedFilter<User, Map<String, ?>> {

	private static final ElementType ELEMENT = ElementType.USER;
	private static final String NAME = "UserFilter";

	private static UserFilter instance;
	public static synchronized UserFilter filter() {
		if (instance == null) {
			instance = new UserFilter();
		}
		return instance;
	}

	private UserFilter() {
		super(NAME, "Filters users", Optional.of(ELEMENT.name()));
	}

	@Override
	protected List<FilterField<User, ?>> getFilters() {
		String owner = ELEMENT.name();
		List<FilterField<User, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibUuidFilter(owner));
		filters.addAll(CommonFields.hibUserTrackingFilter(owner, filter()));
		filters.add(new MappedFilter<>(owner, "username", "Filters by username", StringFilter.filter(), User::getUsername));
		filters.add(new MappedFilter<>(owner, "firstname", "Filters by first name", StringFilter.filter(), User::getFirstname));
		filters.add(new MappedFilter<>(owner, "lastname", "Filters by last name", StringFilter.filter(), User::getLastname));
		filters.add(new MappedFilter<>(owner, "emailAddress", "Filters by email address", StringFilter.filter(), User::getEmailAddress));
		filters.add(new MappedFilter<>(owner, "forcedPasswordChange", "Filters by forced password change flag", BooleanFilter.filter(), User::isForcedPasswordChange));
		return filters;
	}

	@Override
	public GraphQLInputType getType() {
		return GraphQLTypeReference.typeRef(getName());
	}

	@Override
	public GraphQLInputType getSortingType() {
		return GraphQLTypeReference.typeRef(getSortingName());
	}

	@Override
	public final GraphQLInputType createType() {
		return super.getType();
	}

	@Override
	public final GraphQLInputType createSortingType() {
		return super.getSortingType();
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}
}
