package com.gentics.mesh.graphql.filter;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.FilterQuery;
import com.gentics.graphqlfilter.filter.operation.UnformalizableQuery;
import com.gentics.mesh.ElementType;

/**
 * Extension to a start main GraphQl filter for easier FilterOperation creation.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public abstract class EntityFilter<T> extends StartMainFilter<T> {

	public EntityFilter(String name, String description, Optional<String> ownerType) {
		super(name, description, ownerType);
	}

	/**
	 * Create a filter operation out of GraphQL filter argument.
	 * 
	 * @param filterArgument
	 * @return
	 * @throws UnformalizableQuery
	 */
	public FilterOperation<?> createFilterOperation(Map<String, ?> filterArgument) throws UnformalizableQuery  {
		return createFilterOperation(new FilterQuery<>(getEntityType(), getName(), StringUtils.EMPTY, filterArgument));
	}

	/**
	 * Get servered root entity type.
	 * 
	 * @return
	 */
	protected abstract ElementType getEntityType();
}
