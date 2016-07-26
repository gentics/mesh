package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NumberUtils;

/**
 * A {@link PagingParameters} can be used to add additional paging parameters to the rest requests.
 */
public class PagingParameters extends AbstractParameters {

	public static final String PAGE_PARAMETER_KEY = "page";
	public static final String PER_PAGE_PARAMETER_KEY = "perPage";
	public static final String SORT_BY_PARAMETER_KEY = "sortBy";
	public static final String SORT_ORDER_PARAMETER_KEY = "order";

	public static final int DEFAULT_PAGE = 1;

	public PagingParameters(ActionContext ac) {
		super(ac);

		// Validate settings
		int page = getPage();
		if (page < 1) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
		int perPage = getPerPage();
		if (perPage < 0) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
	}

	public PagingParameters(int page, int perPage, String sortBy, SortOrder order) {
		super();
		setPage(page);
		setPerPage(perPage);
		setSortOrder(order.toString());
		setOrderBy(sortBy);
	}

	/**
	 * Create a new paging info for page one.
	 */
	public PagingParameters() {
		this(1);
	}

	/**
	 * Create a new paging info for the given page.
	 * 
	 * @param page
	 *            Page number
	 */
	public PagingParameters(int page) {
		// TODO use reference for default page size
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
	public PagingParameters(int page, int perPage) {
		this(page, perPage, "uuid", SortOrder.ASCENDING);
	}

	/**
	 * Return the current page.
	 * 
	 * @return Current page number
	 */
	public int getPage() {
		return NumberUtils.toInt(getParameter(PAGE_PARAMETER_KEY), DEFAULT_PAGE);
	}

	/**
	 * Return the per page count.
	 * 
	 * @return Per page count
	 */
	public int getPerPage() {
		return NumberUtils.toInt(getParameter(PER_PAGE_PARAMETER_KEY), MeshOptions.DEFAULT_PAGE_SIZE);
	}

	/**
	 * Set the current page.
	 * 
	 * @param page
	 *            Current page number
	 * @return Fluent API
	 */
	public PagingParameters setPage(int page) {
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
	public PagingParameters setPerPage(int perPage) {
		setParameter(PER_PAGE_PARAMETER_KEY, String.valueOf(perPage));
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
	public PagingParameters setSortOrder(String sortBy) {
		setParameter(SORT_BY_PARAMETER_KEY, sortBy);
		return this;
	}

	/**
	 * Set the order by parameter.
	 * 
	 * @param orderBy
	 * @return
	 * @deprecated not yet implemented
	 */
	@Deprecated
	PagingParameters setOrderBy(String orderBy) {
		setParameter(SORT_BY_PARAMETER_KEY, orderBy);
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
		return getParameter(SORT_BY_PARAMETER_KEY);
	}

	/**
	 * Return the order parameter value.
	 * 
	 * @return Sort order
	 * @deprecated not yet implemented
	 */
	@Deprecated
	public SortOrder getOrder() {
		return SortOrder.valueOfName(getParameter(SORT_ORDER_PARAMETER_KEY));
	}

	@Override
	public void validate() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();
		// page
		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDefaultValue(String.valueOf(DEFAULT_PAGE));
		pageParameter.setDescription("Number of page to be loaded.");
		pageParameter.setExample("42");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.NUMBER);
		parameters.put(PAGE_PARAMETER_KEY, pageParameter);

		// perPage
		QueryParameter perPageParameter = new QueryParameter();
		perPageParameter.setDefaultValue(String.valueOf(MeshOptions.DEFAULT_PAGE_SIZE));
		perPageParameter.setDescription("Number of elements per page.");
		perPageParameter.setExample("42");
		perPageParameter.setRequired(false);
		perPageParameter.setType(ParamType.NUMBER);
		parameters.put(PER_PAGE_PARAMETER_KEY, perPageParameter);
		return parameters;
	}

}
