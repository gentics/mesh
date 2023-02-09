package com.gentics.mesh.parameter;

import com.gentics.mesh.core.rest.SortOrder;

/**
 * Interface for sorting query parameters.
 * 
 * @author plyhun
 *
 */
public interface SortingParameters extends ParameterProvider {

	String SORT_ORDER_PARAMETER_KEY = "order";
	String SORT_BY_PARAMETER_KEY = "sortBy";
	SortOrder DEFAULT_SORT_ORDER = SortOrder.UNSORTED;

	/**
	 * Return the sort by parameter value.
	 * 
	 * @return Field to be sorted by
	 */
	default String getSortBy() {
		return getParameter(SortingParameters.SORT_BY_PARAMETER_KEY);
	}

	/**
	 * Return the sortorder.
	 * 
	 * @return
	 */
	default SortOrder getOrder() {
		return SortOrder.valueOfName(getParameter(SortingParameters.SORT_ORDER_PARAMETER_KEY));

	}

	/**
	 * Set the sort by parameter.
	 * 
	 * @param sortBy
	 * @return
	 */
	default SortingParameters setSortBy(String sortBy) {
		setParameter(SortingParameters.SORT_BY_PARAMETER_KEY, sortBy);
		return this;
	}

	/**
	 * Set the used sort order.
	 * 
	 * @param sortBy
	 *            Sort order
	 * @return Fluent API
	 * 
	 */
	default SortingParameters setSortOrder(String sortBy) {
		setParameter(SortingParameters.SORT_ORDER_PARAMETER_KEY, sortBy);
		return this;
	}
}
