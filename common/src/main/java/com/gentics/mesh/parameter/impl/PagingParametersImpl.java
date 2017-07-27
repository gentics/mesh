package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.NumberUtils;

/**
 * A {@link PagingParametersImpl} can be used to add additional paging parameters to the rest requests.
 */
public class PagingParametersImpl extends AbstractParameters implements PagingParameters {

	public static final String PAGE_PARAMETER_KEY = "page";
	public static final String PER_PAGE_PARAMETER_KEY = "perPage";
	public static final String SORT_BY_PARAMETER_KEY = "sortBy";
	public static final String SORT_ORDER_PARAMETER_KEY = "order";

	public static final int DEFAULT_PAGE = 1;
	public static final int DEFAULT_PAGE_SIZE = 25;

	public PagingParametersImpl(ActionContext ac) {
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

	public PagingParametersImpl(long page, int perPage, String sortBy, SortOrder order) {
		super();
		setPage(page);
		setPerPage(perPage);
		setSortOrder(order.toString());
		setOrderBy(sortBy);
	}

	/**
	 * Create a new paging info for page one.
	 */
	public PagingParametersImpl() {
		this(1);
	}

	/**
	 * Create a new paging info for the given page.
	 * 
	 * @param page
	 *            Page number
	 */
	public PagingParametersImpl(int page) {
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
	public PagingParametersImpl(int page, int perPage) {
		this(page, perPage, "uuid", SortOrder.ASCENDING);
	}

	@Override
	public int getPage() {
		return NumberUtils.toInt(getParameter(PAGE_PARAMETER_KEY), DEFAULT_PAGE);
	}

	@Override
	public int getPerPage() {
		return NumberUtils.toInt(getParameter(PER_PAGE_PARAMETER_KEY), DEFAULT_PAGE_SIZE);
	}

	@Override
	public PagingParameters setPage(long page) {
		setParameter(PAGE_PARAMETER_KEY, String.valueOf(page));
		return this;
	}

	@Override
	public PagingParameters setPerPage(int perPage) {
		setParameter(PER_PAGE_PARAMETER_KEY, String.valueOf(perPage));
		return this;
	}

	@Override
	public PagingParameters setSortOrder(String sortBy) {
		setParameter(SORT_BY_PARAMETER_KEY, sortBy);
		return this;
	}

	@Override
	public PagingParameters setOrderBy(String orderBy) {
		setParameter(SORT_BY_PARAMETER_KEY, orderBy);
		return this;
	}

	@Override
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
	public String getName() {
		return "Paging parameters";
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
		perPageParameter.setDefaultValue(String.valueOf(DEFAULT_PAGE_SIZE));
		perPageParameter.setDescription("Number of elements per page.");
		perPageParameter.setExample("42");
		perPageParameter.setRequired(false);
		perPageParameter.setType(ParamType.NUMBER);
		parameters.put(PER_PAGE_PARAMETER_KEY, perPageParameter);
		return parameters;
	}

}
