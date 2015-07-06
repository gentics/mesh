package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;

public class NumberFieldSchemaImpl extends AbstractFieldSchema implements NumberFieldSchema {

	private Integer min;

	private Integer max;

	private Float step;

	private String defaultNumber;

	@Override
	public Integer getMax() {
		return max;
	}

	@Override
	public void setMax(Integer max) {
		this.max = max;
	}

	@Override
	public Integer getMin() {
		return min;
	}

	@Override
	public void setMin(Integer min) {
		this.min = min;
	}

	@Override
	public Float getStep() {
		return step;
	}

	@Override
	public void setStep(Float step) {
		this.step = step;
	}

	@Override
	public String getNumber() {
		return defaultNumber;
	}

	@Override
	public void setNumber(String number) {
		this.defaultNumber = number;
	}

	@Override
	public String getType() {
		return FieldTypes.NUMBER.toString();
	}
}
