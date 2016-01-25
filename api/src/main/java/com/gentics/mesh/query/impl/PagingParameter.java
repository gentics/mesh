package com.gentics.mesh.query.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Map;

import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.util.HttpQueryUtils;
import com.gentics.mesh.util.NumberUtils;

/**
 * A {@link PagingParameter} can be used to add additional paging parameters to the rest requests.
 */
public class PagingParameter implements QueryParameterProvider {

	public static final String PAGE_PARAMETER_KEY = "page";
	public static final String PER_PAGE_PARAMETER_KEY = "perPage";

	private int page;
	private int perPage;
	private String sortBy;
	private SortOrder order;

	public PagingParameter(int page, int perPage, String sortBy, SortOrder order) {
		this.page = page;
		this.perPage = perPage;
		this.sortBy = sortBy;
		this.order = order;
	}

	/**
	 * Create a new paging info for page one.
	 */
	public PagingParameter() {
		this(1);
	}

	/**
	 * Create a new paging info for the given page.
	 * 
	 * @param page
	 *            Page number
	 */
	public PagingParameter(int page) {
		//TODO use reference for default page size
		this(page, 25);
	}

	/**
	 * Create a new paging info with the given values.
	 * 
	 * @param page
	 *            Page number
	 * @param perPage
	 *            Per page count
	 */
	public PagingParameter(int page, int perPage) {
		this(page, perPage, "uuid", SortOrder.ASCENDING);
	}

	/**
	 * Return the current page.
	 * 
	 * @return Current page number
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Return the per page count.
	 * 
	 * @return Per page count
	 */
	public int getPerPage() {
		return perPage;
	}

	/**
	 * Set the current page.
	 * 
	 * @param page
	 *            Current page number
	 * @return Fluent API
	 */
	public PagingParameter setPage(int page) {
		this.page = page;
		return this;
	}

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 *            Per page count
	 * @return Fluent API
	 */
	public PagingParameter setPerPage(int perPage) {
		this.perPage = perPage;
		return this;

	}

	/**
	 * Set the used sort order.
	 * 
	 * @param sortBy
	 *            Sort order
	 * @return Fluent API
	 * @deprecated not yet implemented
	 * 
	 */
	@Deprecated
	public PagingParameter setSortOrder(String sortBy) {
		this.sortBy = sortBy;
		return this;
	}

	/**
	 * Return the sort by parameter value.
	 * 
	 * @return Field to be sorted by
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public String getSortBy() {
		return sortBy;
	}

	/**
	 * Return the order parameter value.
	 * 
	 * @return Sort order
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public SortOrder getOrder() {
		return order;
	}

	@Override
	public String getQueryParameters() {
		//TODO add the other parameters as well
		return PAGE_PARAMETER_KEY + "=" + page + "&" + PER_PAGE_PARAMETER_KEY + "=" + perPage;
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}

	public static PagingParameter fromQuery(String query) {
		Map<String, String> queryParameters = HttpQueryUtils.splitQuery(query);
		String page = queryParameters.get(PAGE_PARAMETER_KEY);
		String perPage = queryParameters.get(PER_PAGE_PARAMETER_KEY);
		int pageInt = 1;
		int perPageInt = MeshOptions.DEFAULT_PAGE_SIZE;
		if (page != null) {
			pageInt = NumberUtils.toInt(page, 1);
		}
		if (perPage != null) {
			perPageInt = NumberUtils.toInt(perPage, MeshOptions.DEFAULT_PAGE_SIZE);
		}
		if (pageInt < 1) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
		if (perPageInt < 0) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
		return new PagingParameter(pageInt, perPageInt);
	}

}
