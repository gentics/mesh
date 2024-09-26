package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * A {@link PagingParametersImpl} can be used to add additional paging parameters to the rest requests.
 */
public class PagingParametersImpl extends SortingParametersImpl implements PagingParameters {


	public PagingParametersImpl(ActionContext ac) {
		super(ac);

		// Validate settings
		int page = getPage();
		if (page < 1) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
		Long perPage = getPerPage();
		if (perPage != null && perPage < 0) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
	}

	public PagingParametersImpl(long page, Long perPage, String sortBy, SortOrder order) {
		super(sortBy, order);
		setPage(page);
		setPerPage(perPage);
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
		this(page, null);
	}

	/**
	 * Create a new paging info with the given values.
	 * 
	 * @param page
	 *            Page number
	 * @param perPage
	 *            Per page count
	 */
	public PagingParametersImpl(int page, Long perPage) {
		this(page, perPage, StringUtils.EMPTY, SortOrder.UNSORTED);
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
		Map<String, QueryParameter> parameters = new HashMap<>(super.getRAMLParameters());
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
		perPageParameter.setDescription("Number of elements per page.");
		perPageParameter.setExample("42");
		perPageParameter.setRequired(false);
		perPageParameter.setType(ParamType.NUMBER);
		parameters.put(PER_PAGE_PARAMETER_KEY, perPageParameter);

		return parameters;
	}

}
