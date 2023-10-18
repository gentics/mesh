package com.gentics.mesh.graphql.filter;

import java.util.ArrayList;
import java.util.List;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.project.HibProject;

import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLTypeReference;

/**
 * Start project GraphQL filter
 */
public class ProjectFilter extends StartMainFilter<HibProject> {

	private static final String NAME = "ProjectFilter";

	private static ProjectFilter instance;
	public static synchronized ProjectFilter filter() {
		if (instance == null) {
			instance = new ProjectFilter();
		}
		return instance;
	}

	private final boolean byRef;

	private ProjectFilter() {
		this(false);
	}

	private ProjectFilter(boolean byRef) {
		super(NAME, "Filters projects");
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
	protected List<FilterField<HibProject, ?>> getFilters() {
		List<FilterField<HibProject, ?>> filters = new ArrayList<>();
		filters.add(CommonFields.hibUuidFilter());
		filters.addAll(CommonFields.hibUserTrackingFilter());
		filters.add(new MappedFilter<>("name", "Filters by name", StringFilter.filter(), HibProject::getName));
		return filters;
	}
}
