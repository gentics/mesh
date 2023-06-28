package com.gentics.mesh.parameter;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

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
	 * Get the sort parameters map.
	 * 
	 * @return
	 */
	default Map<String, SortOrder> getSort() {
		String sortBy = getParameter(SortingParameters.SORT_BY_PARAMETER_KEY);
		SortOrder sortOrder = SortOrder.valueOfName(getParameter(SortingParameters.SORT_ORDER_PARAMETER_KEY));
		if (StringUtils.isNotBlank(sortBy) && sortOrder != null) {
			return Collections.singletonMap(sortBy, sortOrder);
		} else {
			return Collections.emptyMap();
		}
	}

	/**
	 * Put a sort map.
	 * 
	 * @param sortBy
	 * @param order
	 * @return
	 */
	default SortingParameters putSort(String sortBy, SortOrder order) {
		setParameter(SortingParameters.SORT_BY_PARAMETER_KEY, sortBy);
		setParameter(SortingParameters.SORT_ORDER_PARAMETER_KEY, order.toString());
		return this;
	}

	default SortingParameters clearSort() {
		setParameter(SortingParameters.SORT_BY_PARAMETER_KEY, null);
		setParameter(SortingParameters.SORT_ORDER_PARAMETER_KEY, null);
		return this;
	}
}
