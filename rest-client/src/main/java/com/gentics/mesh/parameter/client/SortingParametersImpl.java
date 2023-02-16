package com.gentics.mesh.parameter.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.parameter.SortingParameters;

public class SortingParametersImpl extends AbstractParameters implements SortingParameters {

	private final Map<String, SortOrder> sort = new HashMap<>();

	@Override
	public SortingParameters putSort(String sortBy, SortOrder order) {
		sort.put(sortBy, order);
		return this;
	}

	@Override
	public Map<String, SortOrder> getSort() {
		return Stream.of(sort.entrySet().stream(), SortingParameters.super.getSort().entrySet().stream())
				.flatMap(Function.identity())
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (a, b) -> a));
	}
}
