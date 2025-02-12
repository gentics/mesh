package com.gentics.mesh.parameter;

import com.gentics.mesh.util.NumberUtils;

/**
 * Interface for paging query parameters.
 */
public interface PagingParameters extends ParameterProvider, SortingParameters {

	public static final String PAGE_PARAMETER_KEY = "page";
	public static final String PER_PAGE_PARAMETER_KEY = "perPage";
	public static final int DEFAULT_PAGE = 1;

	/**
	 * Return the current page.
	 * 
	 * @return Current page number
	 */
	default int getPage() {
		return NumberUtils.toInt(getParameter(PAGE_PARAMETER_KEY), DEFAULT_PAGE);
	}

	/**
	 * Return the per page count.
	 * 
	 * @return Per page count
	 */
	default Long getPerPage() {
		return NumberUtils.toLong(getParameter(PER_PAGE_PARAMETER_KEY), null);
	}

	/**
	 * Set the current page.
	 * 
	 * @param page
	 *            Current page number
	 * @return Fluent API
	 */
	default PagingParameters setPage(long page) {
		setParameter(PAGE_PARAMETER_KEY, String.valueOf(page));
		return this;
	}

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 *            Per page count
	 * @return Fluent API
	 */
	default PagingParameters setPerPage(Long perPage) {
		if (perPage != null) {
			setParameter(PER_PAGE_PARAMETER_KEY, String.valueOf(perPage));
		}
		return this;
	}

	/**
	 * Get zero-based page value. Useful to insert into the zero-based paged data fetching mechanisms.
	 * 
	 * @return
	 */
	default int getActualPage() {
		int page = getPage();
		return page < 1 ? 0 : (page - 1);
	}
}
