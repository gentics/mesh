package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.SortingParameters;

/**
 * The sorting parameters may be added to the REST request.
 * 
 * @author plyhun
 *
 */
public class SortingParametersImpl extends AbstractParameters implements SortingParameters {

	private final Map<String, SortOrder> sort = new LinkedHashMap<>();

	public SortingParametersImpl() {
	}

	public SortingParametersImpl(String sortBy, SortOrder order) {
		super();
		putSort(sortBy, order);
	}

	public SortingParametersImpl(ActionContext ac) {
		super(ac);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// sort by
		QueryParameter sortByParameter = new QueryParameter();
		sortByParameter.setDescription("Field name to sort the result by.");
		sortByParameter.setExample("name");
		sortByParameter.setRequired(false);
		sortByParameter.setType(ParamType.STRING);
		parameters.put(SortingParameters.SORT_BY_PARAMETER_KEY, sortByParameter);

		// sort order
		QueryParameter sortOrderParameter = new QueryParameter();
		sortOrderParameter.setDescription("Field order (ASC/DESC/UNSORTED) to sort the result by.");
		sortOrderParameter.setDefaultValue(SortingParameters.DEFAULT_SORT_ORDER.name());
		sortOrderParameter.setExample(SortOrder.ASCENDING.name());
		sortOrderParameter.setRequired(false);
		sortOrderParameter.setType(ParamType.STRING);
		parameters.put(SortingParameters.SORT_ORDER_PARAMETER_KEY, sortOrderParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Sorting parameters";
	}

	@Override
	public SortingParameters putSort(String sortBy, SortOrder order) {
		if (StringUtils.isNotBlank(sortBy) && order != null) {
			sort.put(sortBy, order);
			setParameter(SORT_BY_PARAMETER_KEY, sortBy);
			setParameter(SORT_ORDER_PARAMETER_KEY, order.getValue());
		}
		return this;
	}

	public SortingParameters putSort(Map<String, SortOrder> otherSort) {
		sort.putAll(otherSort);
		return this;
	}

	@Override
	public Map<String, SortOrder> getSort() {
		return Stream.of(sort.entrySet().stream(), SortingParameters.super.getSort().entrySet().stream())
				.flatMap(Function.identity())
				// TODO proper SQL de-inject by param whitelisting
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	public SortingParameters clearSort() {
		sort.clear();
		return SortingParameters.super.clearSort();
	}

	@Override
	public void validate() {
		String sortParam = getParameter(SORT_BY_PARAMETER_KEY);
		if (Optional.ofNullable(sortParam).filter(sort -> sort.matches("[\\s\\;\\r\\n]")).isPresent()) {
			throw error(BAD_REQUEST, "error_invalid_parameter", SORT_BY_PARAMETER_KEY, sortParam);
		}
		super.validate();
	}
}
