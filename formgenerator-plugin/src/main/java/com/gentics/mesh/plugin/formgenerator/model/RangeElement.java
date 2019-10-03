package com.gentics.mesh.plugin.formgenerator.model;

import java.util.stream.Collectors;

import io.vertx.core.MultiMap;

/**
 * Range element
 */
@Reportable
public class RangeElement extends FormElement {
	@Override
	public String getSingleFormData(MultiMap postedData) {
		return getMultiFormData(postedData).stream().collect(Collectors.joining(" "));
	}
}
